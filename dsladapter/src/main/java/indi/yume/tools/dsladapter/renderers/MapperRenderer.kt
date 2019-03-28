package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class MapperRenderer<T, D, VD : ViewData<D>>(
        val mapper: (T) -> D,
        val demapper: (oldData: T, newSub: D)-> T,
        val targetRenderer: BaseRenderer<D, VD>
) : BaseRenderer<T, MapperViewData<T, D, VD>>() {
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
inline fun <T, D, VD : ViewData<D>> BaseRenderer<T, MapperViewData<T, D, VD>>.fix(): MapperRenderer<T, D, VD> =
        this as MapperRenderer<T, D, VD>