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


sealed class RxAdapterEvent<T, VD : ViewData<T>, UP : Updatable<T, VD>> {
    abstract val adapter: RendererAdapter<T, VD, UP>
}

data class AdapterEvent<T, VD : ViewData<T>, UP : Updatable<T, VD>>(override val adapter: RendererAdapter<T, VD, UP>) : RxAdapterEvent<T, VD, UP>()

data class UpdateEvent<T, VD : ViewData<T>, UP : Updatable<T, VD>>(override val adapter: RendererAdapter<T, VD, UP>,
                                                                   val data: T) : RxAdapterEvent<T, VD, UP>()


internal sealed class Option<out T> {
    object None : Option<Nothing>()
    data class Some<T>(val t: T) : Option<T>()
}


class RxBuilder<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val dataProvider: Observable<DL>,
        val composeBuilder: ComposeBuilder<DL, IL, VDL>
) {
    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
            add(supplier: Supplier<T>, renderer: BaseRenderer<T, VD, UP>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, UP>, IL>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>> =
            add(Observable.fromCallable(supplier), renderer)

    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
            add(obs: Observable<T>, renderer: BaseRenderer<T, VD, UP>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, UP>, IL>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>> =
            RxBuilder(
                    dataProvider = Observable.combineLatest(dataProvider, obs,
                            BiFunction<DL, T, HConsK<ForIdT, T, DL>> { otherData, data ->
                                otherData.extend(data.toIdT())
                            }),
                    composeBuilder = composeBuilder.add(renderer))

    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
            add(initData: T, obs: Observable<T>, renderer: BaseRenderer<T, VD, UP>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, UP>, IL>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>> =
            add(Observable.concat(Observable.just(initData), obs), renderer)

    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
            addStatic(initData: T, renderer: BaseRenderer<T, VD, UP>)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, UP>, IL>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>> =
            RxBuilder(
                    dataProvider.map { it.extend(initData.toIdT()) },
                    composeBuilder.add(renderer))

    fun <VD : ViewData<Unit>, UP : Updatable<Unit, VD>>
            addStatic(renderer: BaseRenderer<Unit, VD, UP>)
            : RxBuilder<HConsK<ForIdT, Unit, DL>, HConsK<ForComposeItem, Pair<Unit, UP>, IL>, HConsK<ForComposeItemData, Pair<Unit, UP>, VDL>> =
            RxBuilder(dataProvider.map { it.extend(Unit.toIdT()) }, composeBuilder.add(renderer))

    @CheckResult
    fun build(): Observable<RxAdapterEvent<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>> {
        val renderer = composeBuilder.build()
        return dataProvider
                .scan<Option<RxAdapterEvent<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.ofType<Option.Some<RxAdapterEvent<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>>>()
                .map { it.t }
    }

    @CheckResult
    fun buildAutoUpdate(f: (RendererAdapter<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>) -> Unit)
            : Completable =
            build().flatMap { event ->
                when (event) {
                    is AdapterEvent -> Observable.fromCallable { f(event.adapter) }
                    is UpdateEvent -> Observable.fromCallable { event.adapter.setData(event.data) }
                                .subscribeOn(AndroidSchedulers.mainThread())
                }
            }.ignoreElements()

    companion object {
        val start: RxBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> =
                RxBuilder(Observable.just(HListK.nil()), ComposeRenderer.startBuild)
    }
}

@CheckResult
fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
        RendererAdapter.Companion.singleRxAutoUpdate(
        obs: Observable<T>, renderer: BaseRenderer<T, VD, UP>,
        f: (RendererAdapter<T, VD, UP>) -> Unit): Completable =
        obs
                .scan<Option<RxAdapterEvent<T, VD, UP>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.ofType<Option.Some<RxAdapterEvent<T, VD, UP>>>()
                .map { it.t }
                .flatMap { event ->
                    when (event) {
                        is AdapterEvent -> Observable.fromCallable { f(event.adapter) }
                        is UpdateEvent -> Observable.fromCallable { event.adapter.setData(event.data) }
                                .subscribeOn(AndroidSchedulers.mainThread())
                    }
                }.ignoreElements()


fun RendererAdapter.Companion.rxBuild(): RxBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> = RxBuilder.start
