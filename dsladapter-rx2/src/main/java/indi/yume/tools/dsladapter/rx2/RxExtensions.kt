package indi.yume.tools.dsladapter.rx2

import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.HConsK
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.datatype.HNilK
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


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
        val adapterLife: Completable,
        val dataProvider: Observable<DL>,
        val composeBuilder: ComposeBuilder<DL, IL, VDL>
) {
    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>
            add(supplier: Supplier<T>, renderer: BR)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, BR>, IL>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>> =
            add(Observable.fromCallable(supplier), renderer)

    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>
            add(obs: Observable<T>, renderer: BR)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, BR>, IL>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>> =
            RxBuilder(adapterLife = adapterLife.mergeWith(obs.ignoreElements()),
                    dataProvider = obs.flatMap { initData ->
                        dataProvider.map { it.extend(initData.toIdT()) }
                    },
                    composeBuilder = composeBuilder.add(renderer))

    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>
            add(initData: T, obs: Observable<T>, renderer: BR)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, BR>, IL>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>> =
            add(Observable.concat(Observable.just(initData), obs), renderer)

    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>
            addStatic(initData: T, renderer: BR)
            : RxBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, BR>, IL>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>> =
            RxBuilder(adapterLife,
                    dataProvider.map { it.extend(initData.toIdT()) },
                    composeBuilder.add(renderer))

    fun <VD : ViewData<Unit>, UP : Updatable<Unit, VD>, BR : BaseRenderer<Unit, VD, UP>>
            addStatic(renderer: BR)
            : RxBuilder<HConsK<ForIdT, Unit, DL>, HConsK<ForComposeItem, Pair<Unit, BR>, IL>, HConsK<ForComposeItemData, Pair<Unit, BR>, VDL>> =
            RxBuilder(adapterLife, dataProvider.map { it.extend(Unit.toIdT()) }, composeBuilder.add(renderer))

    fun build(): Observable<RxAdapterEvent<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>> {
        val renderer = composeBuilder.build()
        return dataProvider
                .scan<Option<RxAdapterEvent<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.filter { it is Option.None }.map { (it as Option.Some).t }
    }


    fun buildAutoUpdate(f: (RendererAdapter<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>) -> Unit,
                        computation: Scheduler = Schedulers.computation())
            : Completable =
            build().flatMap { event ->
                when (event) {
                    is AdapterEvent -> Observable.fromCallable { f(event.adapter) }
                    is UpdateEvent -> Observable.fromCallable { event.adapter.setData(event.data) }
                                .subscribeOn(computation)
                                .observeOn(AndroidSchedulers.mainThread())
                }
            }.ignoreElements()

    companion object {
        val start: RxBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> =
                RxBuilder(Completable.complete(), Observable.empty(), ComposeRenderer.startBuild)
    }
}


fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>
        RendererAdapter.Companion.singleRxAutoUpdate(
        obs: Observable<T>, renderer: BR,
        f: (RendererAdapter<T, VD, UP>) -> Unit,
        computation: Scheduler = Schedulers.computation()): Completable =
        obs
                .scan<Option<RxAdapterEvent<T, VD, UP>>>(Option.None)
                { result, item ->
                    when (result) {
                        is Option.None -> Option.Some(AdapterEvent(RendererAdapter(item, renderer)))
                        is Option.Some -> Option.Some(UpdateEvent(result.t.adapter, item))
                    }
                }.filter { it is Option.None }.map { (it as Option.Some).t }
                .flatMap { event ->
                    when (event) {
                        is AdapterEvent -> Observable.fromCallable { f(event.adapter) }
                        is UpdateEvent -> Observable.fromCallable { event.adapter.setData(event.data) }
                                .subscribeOn(computation)
                                .observeOn(AndroidSchedulers.mainThread())
                    }
                }.ignoreElements()


fun RendererAdapter.Companion.rxBuild(): RxBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> = RxBuilder.start
