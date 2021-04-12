package indi.yume.tools.dsladapter.livedata

import androidx.annotation.CheckResult
import androidx.lifecycle.*
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.HConsK
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.datatype.HNilK
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import kotlinx.atomicfu.atomic


sealed class LiveDataAdapterEvent<T, VD : ViewData<T>> {
    abstract val adapter: RendererAdapter<T, VD>
}

data class AdapterEvent<T, VD : ViewData<T>>(override val adapter: RendererAdapter<T, VD>) : LiveDataAdapterEvent<T, VD>()

data class UpdateEvent<T, VD : ViewData<T>>(override val adapter: RendererAdapter<T, VD>,
                                            val data: T) : LiveDataAdapterEvent<T, VD>()


internal sealed class Option<out T> {
    object None : Option<Nothing>()
    data class Some<T>(val t: T) : Option<T>()
}


class LiveDataBuilder<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        val dataProvider: LiveData<DL>,
        val composeBuilder: ComposeBuilder<DL, VDL>
) {
    fun <T, VD : ViewData<T>>
            add(obs: LiveData<T>, renderer: BaseRenderer<T, VD>)
            : LiveDataBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            LiveDataBuilder(
                    dataProvider = margeLiveData(dataProvider, obs) { otherData, data ->
                                otherData.extend(data.toIdT())
                            },
                    composeBuilder = composeBuilder.add(renderer))

    fun <T, VD : ViewData<T>>
            addStatic(initData: T, renderer: BaseRenderer<T, VD>)
            : LiveDataBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            LiveDataBuilder(
                    dataProvider.map { it.extend(initData.toIdT()) },
                    composeBuilder.add(renderer))

    fun <VD : ViewData<Unit>>
            addStatic(renderer: BaseRenderer<Unit, VD>)
            : LiveDataBuilder<HConsK<ForIdT, Unit, DL>, HConsK<ForComposeItemData, Pair<Unit, VD>, VDL>> =
            LiveDataBuilder(dataProvider.map { it.extend(Unit.toIdT()) }, composeBuilder.add(renderer))

    @CheckResult
    fun build(): LiveData<LiveDataAdapterEvent<DL, ComposeViewData<DL, VDL>>> {
        val renderer = composeBuilder.build()
        return dataProvider
                .scan<DL, Option<LiveDataAdapterEvent<DL, ComposeViewData<DL, VDL>>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.ofType<Option.Some<LiveDataAdapterEvent<DL, ComposeViewData<DL, VDL>>>>()
                .map { it.t }
    }

    @CheckResult
    fun buildAutoUpdate(f: (RendererAdapter<DL, ComposeViewData<DL, VDL>>) -> Unit)
            : LiveData<Unit> =
            build().map { event ->
                when (event) {
                    is AdapterEvent -> {
                        f(event.adapter)
                    }
                    is UpdateEvent -> {
                        event.adapter.setData(event.data)
                    }
                }
                Unit
            }

    companion object {
        val start: LiveDataBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> =
                LiveDataBuilder(liveDataOf(HListK.nil()), ComposeRenderer.startBuild)
    }
}

@CheckResult
fun <T, VD : ViewData<T>>
        RendererAdapter.Companion.singleLiveDataAutoUpdate(
        obs: LiveData<T>, renderer: BaseRenderer<T, VD>,
        f: (RendererAdapter<T, VD>) -> Unit): LiveData<Unit> =
        obs
                .scan<T, Option<LiveDataAdapterEvent<T, VD>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.ofType<Option.Some<LiveDataAdapterEvent<T, VD>>>()
                .map { it.t }
                .map { event ->
                    when (event) {
                        is AdapterEvent -> {
                            f(event.adapter)
                        }
                        is UpdateEvent -> {
                            event.adapter.setData(event.data)
                        }
                    }
                    Unit
                }


fun RendererAdapter.Companion.LiveDataBuild(): LiveDataBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> = LiveDataBuilder.start

internal fun <T> liveDataOf(initValue: T): MutableLiveData<T> =
        MutableLiveData(initValue)

internal fun <T1, T2, R> margeLiveData(
        liveData1: LiveData<T1>,
        liveData2: LiveData<T2>,
        zipper: (T1, T2) -> R)
: MediatorLiveData<R> {
    val result = MediatorLiveData<R>()

    result.addSource(liveData1) { t1 ->
        val t2 = liveData2.value
        if (t2 != null)
            result.value = zipper(t1, t2)
    }
    result.addSource(liveData2) { t2 ->
        val t1 = liveData1.value
        if (t1 != null)
            result.value = zipper(t1, t2)
    }
    return result
}

internal fun <T> LiveData<T>.scan(accumulator: (current: T, next: T) -> T): MediatorLiveData<T> {
    val result = MediatorLiveData<T>()
    val currentValue = atomic<T?>(null)

    result.addSource(this@scan) { nextValue ->
        val current = currentValue.value

        if (current != null) {
            val sumValue = accumulator(current, nextValue)
            currentValue.value = sumValue
            result.value = sumValue
        } else {
            currentValue.value = nextValue
        }
    }
    return result
}

internal fun <T, R> LiveData<T>.scan(initData: R, accumulator: (current: R, next: T) -> R): MediatorLiveData<R> {
    val result = MediatorLiveData<R>()

    result.addSource(this@scan) { nextValue ->
        val current = result.value ?: initData

        val sumValue = accumulator(current, nextValue)
        result.value = sumValue
    }
    return result
}

internal fun <T> LiveData<T>.filter(predicate: (T) -> Boolean): LiveData<T> {
    val result = MediatorLiveData<T>()

    result.addSource(this) {
        if (predicate(it))
            result.value = it
    }

    return result
}

@Suppress("UNCHECKED_CAST")
internal fun <T> LiveData<*>.ofType(clazz: Class<T>): LiveData<T> =
        filter { clazz.isInstance(it) }.map { it as T }

internal inline fun <reified T> LiveData<*>.ofType(): LiveData<T> =
        ofType(T::class.java)
