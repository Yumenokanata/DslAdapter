package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.MapperRenderer
import indi.yume.tools.dsladapter.renderers.MapperViewData
import indi.yume.tools.dsladapter.renderers.fix
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD


class MapperUpdater<T, D, VD : ViewData<D>>(
        val renderer: MapperRenderer<T, D, VD>
) : Updatable<T, MapperViewData<T, D, VD>> {
    constructor(base: BaseRenderer<T, MapperViewData<T, D, VD>>): this(base.fix())

    val mapOri = renderer.mapper

    fun <UP : Updatable<D, VD>> mapWithOld(targetUpdatable: (BaseRenderer<D, VD>) -> UP, f: UP.(oldData: T) -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> =
            mapWithOld { targetUpdatable(this).f(it) }

    fun mapWithOld(targetUpdatable: BaseRenderer<D, VD>.(oldData: T) -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> = { oldVD ->
        val mapperAction = targetUpdatable(renderer.targetRenderer, oldVD.originData)
        val (actions, newMapVD) = mapperAction(oldVD.mapVD)

        actions to MapperViewData(renderer.demapper(oldVD.originData, newMapVD.originData), newMapVD)
    }

    fun <UP : Updatable<D, VD>> mapper(updatable: (BaseRenderer<D, VD>) -> UP, f: UP.() -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> =
            mapper { updatable(this).f() }

    fun mapper(updatable: BaseRenderer<D, VD>.() -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> =
            mapWithOld { updatable() }

    fun update(newData: T, payload: Any? = null): ActionU<MapperViewData<T, D, VD>> {
        val newVD = renderer.getData(newData)

        return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<MapperViewData<T, D, VD>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <T, D, VD : ViewData<D>> BaseRenderer<T, MapperViewData<T, D, VD>>.updater
    get() = MapperUpdater(this)

fun <T, D, VD : ViewData<D>> updatable(renderer: BaseRenderer<T, MapperViewData<T, D, VD>>) =
        MapperUpdater(renderer)
