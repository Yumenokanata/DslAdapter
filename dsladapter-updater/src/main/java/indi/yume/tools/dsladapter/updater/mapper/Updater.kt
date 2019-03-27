package indi.yume.tools.dsladapter.updater.mapper

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.MapperRenderer
import indi.yume.tools.dsladapter.renderers.MapperViewData
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD


fun <T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>> MapperRenderer<T, D, VD, BR>.mapWithOld(f: BR.(oldData: T) -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> = { oldVD ->
    val mapperAction = this.targetRenderer.f(oldVD.originData)
    val (actions, newMapVD) = mapperAction(oldVD.mapVD)

    actions to MapperViewData(this.demapper(oldVD.originData, newMapVD.originData), newMapVD)
}

fun <T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>> MapperRenderer<T, D, VD, BR>
        .map(f: BR.() -> ActionU<VD>): ActionU<MapperViewData<T, D, VD>> =
        mapWithOld { f() }

fun <T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>> MapperRenderer<T, D, VD, BR>
        .update(newData: T, payload: Any? = null): ActionU<MapperViewData<T, D, VD>> {
    val newVD = this.getData(newData)

    return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
}

fun <T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>> MapperRenderer<T, D, VD, BR>
        .reduce(f: (oldData: T) -> ChangedData<T>): ActionU<MapperViewData<T, D, VD>> = { oldVD ->
    val (newData, payload) = f(oldVD.originData)
    update(newData, payload)(oldVD)
}