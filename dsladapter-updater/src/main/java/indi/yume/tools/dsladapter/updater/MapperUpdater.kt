package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.MapperRenderer
import indi.yume.tools.dsladapter.renderers.MapperViewData
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD


class MapperUpdater<T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>>(
        val renderer: MapperRenderer<T, D, VD, BR>
) : Updatable<T, MapperViewData<T, D, VD>> {

    val mapOri = renderer.mapper

    fun <UP : Updatable<D, VD>> mapWithOld(targetUpdatable: (BR) -> UP, f: UP.(oldData: T) -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> = { oldVD ->
        val mapperAction = targetUpdatable(renderer.targetRenderer).f(oldVD.originData)
        val (actions, newMapVD) = mapperAction(oldVD.mapVD)

        actions to MapperViewData(renderer.demapper(oldVD.originData, newMapVD.originData), newMapVD)
    }

    fun <UP : Updatable<D, VD>> mapper(updatable: (BR) -> UP, f: UP.() -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> =
            mapWithOld(updatable) { f() }

    fun update(newData: T, payload: Any? = null): ActionU<MapperViewData<T, D, VD>> {
        val newVD = renderer.getData(newData)

        return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<MapperViewData<T, D, VD>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>> MapperRenderer<T, D, VD, BR>.updater
    get() = MapperUpdater(this)

fun <T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>> updatable(renderer: MapperRenderer<T, D, VD, BR>) =
        MapperUpdater(renderer)
