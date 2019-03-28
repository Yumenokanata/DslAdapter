package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import arrow.Kind
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ComposeRenderer<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        val getMapper: (DL) -> VDL
) : BaseRenderer<DL, ComposeViewData<DL, VDL>>() {
    override fun getData(content: DL): ComposeViewData<DL, VDL> {
        return ComposeViewData(content, getMapper(content))
    }

    override fun getItemId(data: ComposeViewData<DL, VDL>, index: Int): Long {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.vdNormalList[resolvedRepositoryIndex]
        return item.renderer.getItemId(vd, resolvedItemIndex)
    }

    override fun getLayoutResId(data: ComposeViewData<DL, VDL>, index: Int): Int {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.vdNormalList[resolvedRepositoryIndex]
        return item.renderer.getLayoutResId(vd, resolvedItemIndex)
    }

    override fun bind(data: ComposeViewData<DL, VDL>, index: Int, holder: RecyclerView.ViewHolder) {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.vdNormalList[resolvedRepositoryIndex]
        item.renderer.bind(vd, resolvedItemIndex, holder)
        holder.bindRecycle(this) { item.renderer.recycle(it) }
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }

    companion object {
        val startBuild: ComposeBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> =
                ComposeBuilder { _ -> HListK.nil() }
    }
}

class ComposeBuilder<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        val getMapper: (DL) -> VDL
) {
    fun <T, VD : ViewData<T>> add(renderer: BaseRenderer<T, VD>)
            : ComposeBuilder<HConsK<ForIdT, T, DL>,
            HConsK<ForComposeItemData, Pair<T, VD>, VDL>> {
        return ComposeBuilder { dl ->
            getMapper(dl.tail)
                    .extend(ComposeItemData(renderer.getData(dl.head.value()), ComposeItem(renderer)))
        }
    }

    val start : ComposeBuilder<DL, VDL> = this

    fun build(): ComposeRenderer<DL, VDL> = ComposeRenderer(getMapper)
}



fun <T, VD : ViewData<T>>
        itemSub(renderer: BaseRenderer<T, VD>)
        : ComposeItem<T, VD> =
        ComposeItem(renderer)

data class ComposeItem<T, VD : ViewData<T>>(
        val renderer: BaseRenderer<T, VD>
) {
    val key = Any()
}

data class ComposeItemData<T, VD : ViewData<T>>(
        val viewData: VD,
        val item: ComposeItem<T, VD>) : ComposeDataOf<T, VD>

//<editor-fold defaultstate="collapsed" desc="ComposeItemData HighType">
class ForComposeItemData private constructor() {
    companion object
}
typealias ComposeDataOf<T, VD> = Kind<ForComposeItemData, Pair<T, VD>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun Kind<ForComposeItemData, Any?>.fixAny(): ComposeItemData<Any?, ViewData<Any?>> =
        this as ComposeItemData<Any?, ViewData<Any?>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD> ComposeDataOf<T, VD>.fix(): ComposeItemData<T, VD> where VD : ViewData<T> =
        this as ComposeItemData<T, VD>
//</editor-fold>

data class ComposeViewData<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        override val originData: DL,
        val vdList: VDL) : ViewData<DL> {
    val vdNormalList: List<ComposeItemData<Any?, ViewData<Any?>>> =
            vdList.toList().map { it.fixAny() }

    val endsPoint: IntArray = vdNormalList.getEndsPoints { it.viewData.count }

    override val count: Int = endsPoint.getEndPoint()
}

fun <DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>> BaseRenderer<DL, ComposeViewData<DL, VDL>>.fix(): ComposeRenderer<DL, VDL> =
        this as ComposeRenderer<DL, VDL>
