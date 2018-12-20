package indi.yume.tools.dsladapter.renderers

import android.support.v7.widget.RecyclerView
import indi.yume.tools.dsladapter.Action
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD

class MapperRenderer<T, D, VD : ViewData<D>, UP : Updatable<D, VD>>(
        val mapper: (T) -> D,
        val demapper: (oldData: T, newSub: D)-> T,
        val targetRenderer: BaseRenderer<D, VD, UP>
) : BaseRenderer<T, MapperViewData<T, D, VD>, MapperUpdater<T, D, VD, UP>>() {
    override val updater: MapperUpdater<T, D, VD, UP> = MapperUpdater(this)

    override fun getData(content: T): MapperViewData<T, D, VD> = MapperViewData(content, targetRenderer.getData(mapper(content)))

    override fun getItemId(data: MapperViewData<T, D, VD>, index: Int): Long = targetRenderer.getItemId(data.mapVD, index)

    override fun getLayoutResId(data: MapperViewData<T, D, VD>, index: Int): Int =
            targetRenderer.getLayoutResId(data.mapVD, index)

    override fun bind(data: MapperViewData<T, D, VD>, index: Int, holder: RecyclerView.ViewHolder) =
            targetRenderer.bind(data.mapVD, index, holder)

    override fun recycle(holder: RecyclerView.ViewHolder) =
            targetRenderer.recycle(holder)

    companion object
}

data class MapperViewData<T, D, VD : ViewData<D>>(override val originData: T, val mapVD: VD) : ViewData<T> {
    override val count: Int = mapVD.count
}


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, D, VD : ViewData<D>, UP : Updatable<D, VD>> BaseRenderer<T, MapperViewData<T, D, VD>, MapperUpdater<T, D, VD, UP>>.fix(): MapperRenderer<T, D, VD, UP> =
        this as MapperRenderer<T, D, VD, UP>

class MapperUpdater<T, D, VD : ViewData<D>, UP : Updatable<D, VD>>(
        val renderer: MapperRenderer<T, D, VD, UP>
) : Updatable<T, MapperViewData<T, D, VD>> {
    fun mapper(f: UP.() -> Action<VD>): Action<MapperViewData<T, D, VD>> = { oldVD ->
        val mapperAction = renderer.targetRenderer.updater.f()
        val (actions, newMapVD) = mapperAction(oldVD.mapVD)

        actions to MapperViewData(renderer.demapper(oldVD.originData, newMapVD.originData), newMapVD)
    }

    fun update(newData: T, payload: Any? = null): Action<MapperViewData<T, D, VD>> {
        val newVD = renderer.getData(newData)

        return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
    }
}
