package indi.yume.tools.dsladapter.rx2

import androidx.annotation.CheckResult
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.HConsK
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.datatype.HNilK
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.ofType


sealed class RxAdapterEvent<T, VD : ViewData<T>> {
    abstract val adapter: RendererAdapter<T, VD>
}

data class AdapterEvent<T, VD : ViewData<T>>(override val adapter: RendererAdapter<T, VD>) : RxAdapterEvent<T, VD>()

data class UpdateEvent<T, VD : ViewData<T>>(override val adapter: RendererAdapter<T, VD>,
                                                                   val data: T) : RxAdapterEvent<T, VD>()


internal sealed class Option<out T> {
    object None : Option<Nothing>()
    data class Some<T>(val t: T) : Option<T>()
}


class RxBuilder<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        val dataProvider: Observable<DL>,
        val composeBuilder: ComposeBuilder<DL, VDL>
) {
    fun <T, VD : ViewData<T>>
            add(supplier: Supplier<T>, renderer: BaseRenderer<T, VD>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            add(Observable.fromCallable(supplier), renderer)

    fun <T, VD : ViewData<T>>
            add(obs: Observable<T>, renderer: BaseRenderer<T, VD>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            RxBuilder(
                    dataProvider = Observable.combineLatest(dataProvider, obs,
                            BiFunction<DL, T, HConsK<ForIdT, T, DL>> { otherData, data ->
                                otherData.extend(data.toIdT())
                            }),
                    composeBuilder = composeBuilder.add(renderer))

    fun <T, VD : ViewData<T>>
            add(initData: T, obs: Observable<T>, renderer: BaseRenderer<T, VD>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            add(Observable.concat(Observable.just(initData), obs), renderer)

    fun <T, VD : ViewData<T>>
            addStatic(initData: T, renderer: BaseRenderer<T, VD>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            RxBuilder(
                    dataProvider.map { it.extend(initData.toIdT()) },
                    composeBuilder.add(renderer))

    fun <VD : ViewData<Unit>>
            addStatic(renderer: BaseRenderer<Unit, VD>)
            : RxBuilder<HConsK<ForIdT, Unit, DL>, HConsK<ForComposeItemData, Pair<Unit, VD>, VDL>> =
            RxBuilder(dataProvider.map { it.extend(Unit.toIdT()) }, composeBuilder.add(renderer))

    @CheckResult
    fun build(): Observable<RxAdapterEvent<DL, ComposeViewData<DL, VDL>>> {
        val renderer = composeBuilder.build()
        return dataProvider
                .scan<Option<RxAdapterEvent<DL, ComposeViewData<DL, VDL>>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.ofType<Option.Some<RxAdapterEvent<DL, ComposeViewData<DL, VDL>>>>()
                .map { it.t }
    }

    @CheckResult
    fun buildAutoUpdate(f: (RendererAdapter<DL, ComposeViewData<DL, VDL>>) -> Unit)
            : Completable =
            build().flatMap { event ->
                when (event) {
                    is AdapterEvent -> Observable.fromCallable { f(event.adapter) }
                    is UpdateEvent -> Observable.fromCallable { event.adapter.setData(event.data) }
                                .subscribeOn(AndroidSchedulers.mainThread())
                }
            }.ignoreElements()

    companion object {
        val start: RxBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> =
                RxBuilder(Observable.just(HListK.nil()), ComposeRenderer.startBuild)
    }
}

@CheckResult
fun <T, VD : ViewData<T>>
        RendererAdapter.Companion.singleRxAutoUpdate(
        obs: Observable<T>, renderer: BaseRenderer<T, VD>,
        f: (RendererAdapter<T, VD>) -> Unit): Completable =
        obs
                .scan<Option<RxAdapterEvent<T, VD>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.ofType<Option.Some<RxAdapterEvent<T, VD>>>()
                .map { it.t }
                .flatMap { event ->
                    when (event) {
                        is AdapterEvent -> Observable.fromCallable { f(event.adapter) }
                        is UpdateEvent -> Observable.fromCallable { event.adapter.setData(event.data) }
                                .subscribeOn(AndroidSchedulers.mainThread())
                    }
                }.ignoreElements()


fun RendererAdapter.Companion.rxBuild(): RxBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> = RxBuilder.start
