package indi.yume.tools.dsladapter.datatype

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import indi.yume.tools.dsladapter.typeclass.ViewData
import java.util.*
import kotlin.collections.ArrayList

sealed class FakeUpdateActions<T>

data class FakeOnInserted<T>(val pos: Int, val count: Int, var insertedItems: LinkedList<T> = LinkedList()) : FakeUpdateActions<T>()

data class FakeOnRemoved<T>(val pos: Int, val count: Int, var removeItems: List<T>? = null) : FakeUpdateActions<T>()

data class FakeOnMoved<T>(val fromPosition: Int, val toPosition: Int, var item: T? = null) : FakeUpdateActions<T>()

data class FakeOnChanged<T>(val pos: Int, val count: Int, val payload: Any?,
                            var oldItems: List<T>? = null, var newItems: LinkedList<T> = LinkedList()) : FakeUpdateActions<T>()

sealed class FakeItem<T>

data class FakeReal<T>(val t: T) : FakeItem<T>()

data class FakeMock<T>(val act: FakeUpdateActions<T>) : FakeItem<T>()


sealed class ListUpdateActions<T> {
    data class OnInserted<T>(val pos: Int, val count: Int, val insertedItems: List<T>) : ListUpdateActions<T>()

    data class OnRemoved<T>(val pos: Int, val count: Int, val removedItems: List<T>) : ListUpdateActions<T>()

    data class OnMoved<T>(val fromPosition: Int, val toPosition: Int, val item: T) : ListUpdateActions<T>()

    data class OnChanged<T>(val pos: Int, val count: Int, val payload: Any?, val oldItems: List<T>, val newItems: List<T>) : ListUpdateActions<T>()
}

fun <T: Any> DiffUtil.DiffResult.toFakeActions(): List<FakeUpdateActions<T>> {
    val recorder = LinkedList<FakeUpdateActions<T>>()
    dispatchUpdatesTo(object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            recorder += FakeOnInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            recorder += FakeOnRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            recorder += FakeOnMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            recorder += FakeOnChanged(position, count, payload)
        }
    })

    return recorder
}

fun <T> List<UpdateActions>.toFakeActions(): List<FakeUpdateActions<T>> =
        map<UpdateActions, FakeUpdateActions<T>?> {
            when (it) {
                is EmptyAction -> null
                is OnInserted -> FakeOnInserted(it.pos, it.count)
                is OnRemoved -> FakeOnRemoved(it.pos, it.count)
                is OnMoved -> FakeOnMoved(it.fromPosition, it.toPosition)
                is OnChanged -> FakeOnChanged(it.pos, it.count, it.payload)
                is ActionComposite -> throw UnsupportedOperationException("updateAuto do not support ActionComposite.")
            }
        }.filterNotNull()

fun <T> List<FakeUpdateActions<T>>.getMockList(oldData: List<T>): List<FakeItem<T>> {
    val fakeOldData: List<FakeItem<T>> = oldData.map { FakeReal(it) }

    val fakeNewData = this.fold(fakeOldData)
    { data, act ->
        val nData: List<FakeItem<T>> = when(act) {
            is FakeOnInserted -> {
                val mock = FakeMock(act)
                data.subList(0, act.pos) + List(act.count, { mock }) + data.subList(act.pos, data.size)
            }
            is FakeOnRemoved -> {
                act.removeItems = data.subList(act.pos, act.pos + act.count).map { (it as FakeReal<T>).t }
                data.subList(0, act.pos) + data.subList(act.pos + act.count, data.size)
            }
            is FakeOnMoved -> {
                act.item = (data[act.fromPosition] as FakeReal<T>).t
                if (act.fromPosition < act.toPosition) {
                    data.subList(0, act.fromPosition) + data.subList(act.fromPosition + 1, act.toPosition + 1) + data[act.fromPosition] + data.subList(act.toPosition + 1, data.size)
                } else {
                    data.subList(0, act.toPosition) + data[act.fromPosition] + data.subList(act.toPosition, act.fromPosition) + data.subList(act.fromPosition + 1, data.size)
                }
            }
            is FakeOnChanged -> {
                act.oldItems = data.subList(act.pos, act.pos + act.count).map { (it as FakeReal<T>).t }
                val mock = FakeMock(act)
                data.subList(0, act.pos) + List(act.count, { mock }) + data.subList(act.pos + act.count, data.size)
            }
        }

        nData
    }

    return fakeNewData
}

fun <T, VD: ViewData<T>> DiffUtil.DiffResult.toActionsWithRealIndex(
        oldData: List<VD>, newData: List<VD>,
        mapChanged: ((FakeOnChanged<VD>, List<Int>) -> UpdateActions)? = null): List<UpdateActions> =
        toFakeActions<VD>().toActionsWithRealIndex(oldData, newData, mapChanged) { it.count }

fun <T> List<FakeUpdateActions<T>>.toActionsWithRealIndex(
        oldData: List<T>, newData: List<T>,
        mapChanged: ((FakeOnChanged<T>, List<Int>) -> UpdateActions)? = null,
        countGet: (T) -> Int): List<UpdateActions> {
    val recorder = this
    val fakeNewData = recorder.getMockList(oldData)
    fakeNewData.putRealData(newData)

    val realActions = ArrayList<UpdateActions>()
    recorder.fold(oldData.map(countGet))
    { data, act ->
        val nData: List<Int> = when(act) {
            is FakeOnInserted -> {
                val startList = data.subList(0, act.pos)
                val insertListCount = act.insertedItems.map(countGet)

                val realPos = startList.sum()
                val count = insertListCount.sum()
                realActions += OnInserted(realPos, count)
                startList + insertListCount + data.subList(act.pos, data.size)
            }
            is FakeOnRemoved -> {
                val startList = data.subList(0, act.pos)
                val realPos = startList.sum()
                val count = act.removeItems!!.sumBy(countGet)
                realActions += OnRemoved(realPos, count)
                startList + data.subList(act.pos + act.count, data.size)
            }
            is FakeOnMoved -> {
                if (act.fromPosition < act.toPosition) {
                    val firstList = data.subList(0, act.fromPosition)
                    val secondList = data.subList(act.fromPosition + 1, act.toPosition + 1)

                    val realCount = countGet(act.item!!)

                    if(realCount == 1) {
                        val realFromPosition = firstList.sum()
                        val realToPosition = realFromPosition + realCount + secondList.sum() - 1

                        realActions += OnMoved(realFromPosition, realToPosition)
                    } else {
                        val realFromPosition = firstList.sum()
                        val realToPosition = realFromPosition + realCount + secondList.sum()

                        realActions += OnInserted(realToPosition, realCount)
                        realActions += OnRemoved(realFromPosition, realCount)
                    }

                    firstList + secondList + realCount + data.subList(act.toPosition, data.size)
                } else {
                    val firstList = data.subList(0, act.toPosition)
                    val secondList = data.subList(act.toPosition, act.fromPosition)

                    val realCount = countGet(act.item!!)

                    if(realCount == 1) {
                        val realToPosition = firstList.sum()
                        val realFromPosition = realToPosition + secondList.sum() - 1

                        realActions += OnMoved(realFromPosition, realToPosition)
                    } else {
                        val realToPosition = firstList.sum()
                        val realFromPosition = realToPosition + secondList.sum()

                        realActions += OnRemoved(realFromPosition, realCount)
                        realActions += OnInserted(realToPosition, realCount)
                    }

                    firstList + realCount + secondList + data.subList(act.fromPosition + 1, data.size)
                }
            }
            is FakeOnChanged -> {
                val firstList = data.subList(0, act.pos)
                val realCountList = act.newItems.map(countGet)

                realActions += mapChanged?.invoke(act, data) ?: OnChanged(firstList.sum(), realCountList.sum(), act.payload)

                firstList + realCountList + data.subList(act.pos + act.count, data.size)
            }
        }

        nData
    }

    return realActions
}

fun <T> List<FakeItem<T>>.putRealData(newData: List<T>): List<FakeItem<T>> {
    val newDataIt = newData.iterator()
    val fakeIt = iterator()
    while (newDataIt.hasNext() && fakeIt.hasNext()) {
        val real = newDataIt.next()
        val fake = fakeIt.next()

        if (fake is FakeMock)
            when (fake.act) {
                is FakeOnInserted -> fake.act.insertedItems.add(real)
                is FakeOnRemoved -> Unit
                is FakeOnMoved -> Unit
                is FakeOnChanged -> fake.act.newItems.add(real)
            }
    }

    return this
}


fun <T: Any> DiffUtil.DiffResult.toActions(oldData: List<T>, newData: List<T>): List<ListUpdateActions<T>> {
    val recorder = toFakeActions<T>()
    val fakeNewData = recorder.getMockList(oldData)

    fakeNewData.putRealData(newData)

    return recorder.map {
        when(it) {
            is FakeOnInserted -> ListUpdateActions.OnInserted(it.pos, it.count, it.insertedItems)
            is FakeOnRemoved -> ListUpdateActions.OnRemoved(it.pos, it.count, it.removeItems!!)
            is FakeOnMoved -> ListUpdateActions.OnMoved(it.fromPosition, it.toPosition, it.item!!)
            is FakeOnChanged -> ListUpdateActions.OnChanged(it.pos, it.count, it.payload, it.oldItems!!, it.newItems)
        }
    }
}