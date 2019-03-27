package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import arrow.Kind
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ComposeRenderer<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val composeList: IL,
        val getMapper: (DL, IL) -> VDL
) : BaseRenderer<DL, ComposeViewData<DL, VDL>>() {
    override fun getData(content: DL): ComposeViewData<DL, VDL> {
        return ComposeViewData(content, getMapper(content, composeList))
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
        val startBuild: ComposeBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> =
                ComposeBuilder(HListK.nil()) { _, _ -> HListK.nil() }
    }
}

class ComposeBuilder<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val composeList: IL,
        val getMapper: (DL, IL) -> VDL
) {
    fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>> add(renderer: BR)
            : ComposeBuilder<HConsK<ForIdT, T, DL>,
            HConsK<ForComposeItem, Pair<T, BR>, IL>,
            HConsK<ForComposeItemData, Pair<T, BR>, VDL>> {
        return ComposeBuilder(composeList.extend(ComposeItem(renderer)))
        { dl, il ->
            val item = il.head.fix()
            getMapper(dl.tail, il.tail)
                    .extend(ComposeItemData(item.renderer.getData(dl.head.value()), item))
        }
    }

    val start : ComposeBuilder<DL, IL, VDL> = this

    fun build(): ComposeRenderer<DL, IL, VDL> = ComposeRenderer(composeList, getMapper)
}



fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>
        itemSub(renderer: BR)
        : ComposeItem<T, VD, BR> =
        ComposeItem(renderer)

data class ComposeItem<T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>(
        val renderer: BR
) : ComposeOf<T, BR> {
    val key = Any()
}

//<editor-fold defaultstate="collapsed" desc="ComposeItem HighType">
class ForComposeItem private constructor() {
    companion object
}
typealias ComposeOf<T, BR> = Kind<ForComposeItem, Pair<T, BR>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Kind<ForComposeItem, Any?>.fixAny(): ComposeItem<Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>> =
        this as ComposeItem<Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD, BR> ComposeOf<T, BR>.fix(): ComposeItem<T, VD, BR> where VD : ViewData<T>, BR : BaseRenderer<T, VD> =
        this as ComposeItem<T, VD, BR>

//</editor-fold>

data class ComposeItemData<T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>(
        val viewData: VD,
        val item: ComposeItem<T, VD, BR>) : ComposeDataOf<T, BR>

//<editor-fold defaultstate="collapsed" desc="ComposeItemData HighType">
class ForComposeItemData private constructor() {
    companion object
}
typealias ComposeDataOf<T, BR> = Kind<ForComposeItemData, Pair<T, BR>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun Kind<ForComposeItemData, Any?>.fixAny(): ComposeItemData<Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>> =
        this as ComposeItemData<Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD, BR> ComposeDataOf<T, BR>.fix(): ComposeItemData<T, VD, BR> where VD : ViewData<T>, BR : BaseRenderer<T, VD> =
        this as ComposeItemData<T, VD, BR>
//</editor-fold>

data class ComposeViewData<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        override val originData: DL,
        val vdList: VDL) : ViewData<DL> {
    val vdNormalList: List<ComposeItemData<Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>>> =
            vdList.toList().map { it.fixAny() }

    val endsPoint: IntArray = vdNormalList.getEndsPoints { it.viewData.count }

    override val count: Int = endsPoint.getEndPoint()
}
