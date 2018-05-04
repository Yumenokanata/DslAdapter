package indi.yume.tools.dsladapter3.renderers

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import indi.yume.tools.dsladapter3.datatype.ActionComposite
import indi.yume.tools.dsladapter3.datatype.FakeOnChanged
import indi.yume.tools.dsladapter3.datatype.UpdateActions
import indi.yume.tools.dsladapter3.datatype.toActionsWithRealIndex
import indi.yume.tools.dsladapter3.typeclass.Renderer
import indi.yume.tools.dsladapter3.typeclass.ViewData
import java.util.*
import kotlin.coroutines.experimental.buildSequence


class Recycler(val f: (RecyclerView.ViewHolder) -> Unit)

fun <T1, T2, T3, V> zip3(it1: Iterable<T1>, it2: Iterable<T2>, it3: Iterable<T3>,
                         zipper: (T1, T2, T3) -> V): Sequence<V> =
        buildSequence {
            val iter1 = it1.iterator()
            val iter2 = it2.iterator()
            val iter3 = it3.iterator()

            while (iter1.hasNext() && iter2.hasNext() && iter3.hasNext()) {
                yield(zipper(iter1.next(), iter2.next(), iter3.next()))
            }
        }

fun <VD: ViewData> List<VD>.getEndsPonints(): IntArray =
        getEndsPonints { it.count }

fun <T> List<T>.getEndsPonints(getter: (T) -> Int): IntArray {
    val ends = IntArray(size)
    var lastEndPosition = 0
    for ((i, vd) in withIndex()) {
        lastEndPosition += getter(vd)
        ends[i] = lastEndPosition
    }

    return ends
}

fun resolveIndices(position: Int, endPos: IntArray): Pair<Int, Int> {
    if (position < 0 || position >= endPos.last()) {
        throw IndexOutOfBoundsException(
                "Asked for position $position while count is ${endPos.last()}")
    }

    var arrayIndex = Arrays.binarySearch(endPos, position)
    if (arrayIndex >= 0) {
        do {
            arrayIndex++
        } while (endPos[arrayIndex] == position)
    } else {
        arrayIndex = arrayIndex.inv()
    }

    val resolvedRepositoryIndex = arrayIndex
    val resolvedItemIndex = if (arrayIndex == 0) position else position - endPos[arrayIndex - 1]

    return resolvedRepositoryIndex to resolvedItemIndex
}

fun <T, VD: ViewData> checkListUpdates(oldData: List<VD>, newData: List<VD>,
                                       keyGetter: (VD, Int) -> Any?,
                                       renderer: Renderer<T, VD>? = null): List<UpdateActions> {
    val mapChanged: ((FakeOnChanged<VD>, List<Int>) -> UpdateActions)? =
            if (renderer == null)
                null
            else
                { act, indexData ->
                    var startIndex = 0
                    var index = 0
                    val actionComposite = ArrayList<UpdateActions>()

                    for (i in 0 until (act.pos + act.count)) {
                        if (i == act.pos)
                            startIndex = index
                        if (i >= act.pos) {
                            val actions = renderer.getUpdates(act.oldItems!![i - act.pos], act.newItems[i - act.pos])
                            if (actions.isNotEmpty())
                                actionComposite += ActionComposite(index, actions)
                        }

                        index += indexData[i]
                    }

                    ActionComposite(startIndex, actionComposite)
                }

    val realActions = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldData.size

        override fun getNewListSize(): Int = newData.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                keyGetter(oldData[oldItemPosition], oldItemPosition) == keyGetter(newData[newItemPosition], newItemPosition)

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldData[oldItemPosition] == newData[newItemPosition]
    }, false).toActionsWithRealIndex(oldData, newData, mapChanged)

    return realActions
}

fun List<UpdateActions>.filterUselessAction(): List<UpdateActions> =
        map {
            if (it is ActionComposite)
                if (it.actions.isEmpty())
                    null
                else {
                    val subActions = it.actions.filterUselessAction()
                    if (subActions.isEmpty())
                        null
                    else
                        ActionComposite(it.offset, subActions)
                }
            else
                it
        }.filterNotNull()
