package indi.yume.demo.dsladapter.position

import android.annotation.SuppressLint
import arrow.Kind
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import indi.yume.tools.dsladapter.ForIdT
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.datatype.get0
import indi.yume.tools.dsladapter.datatype.get1
import indi.yume.tools.dsladapter.forList
import indi.yume.tools.dsladapter.layout
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

data class RendererPos(val start: Int, val count: Int) {
    val end: Int = start + count - 1
}

data class ErrorMsg(val location: List<String>, val msg: String) {
    constructor(localTag: String, msg: String): this(listOf(localTag), msg)

    fun push(localTag: String): ErrorMsg = ErrorMsg(listOf(localTag) + location, msg)

    val formatMsg: String
        get() = "${location.joinToString(separator = "->")}: $msg"
}

typealias PosAction<VD> = (VD) -> Either<ErrorMsg, RendererPos>

fun <T, VD : ViewData<T>> BaseRenderer<T, VD>.pos(f: BaseRenderer<T, VD>.() -> PosAction<VD>): PosAction<VD> =
        { vd -> this@pos.f()(vd) }

fun <T, VD : ViewData<T>> me(): PosAction<VD> = { vd -> RendererPos(0, vd.count).right() }

//<editor-fold desc="ComposeRenderer">
fun <T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>
        BaseRenderer<DL, ComposeViewData<DL, VDL>>.itemPos(getter: VDL.() -> ComposeDataOf<T, VD>,
                                                           f: BaseRenderer<T, VD>.() -> PosAction<VD> = { me() }): PosAction<ComposeViewData<DL, VDL>> =
        result@{ vd ->
            val realRenderer = this@itemPos.fix()
            val composeItemData = getter(vd.vdList).fix()

            val itemPos = vd.vdNormalList.indexOfFirst { it.item.key === composeItemData.item.key }
            if (itemPos == -1)
                return@result ErrorMsg("ComposeRenderer",
                        "Not have match composeItem. composeItemData: $composeItemData; itemList: ${vd.vdNormalList.joinToString()}")
                        .left()

            val itemRealStartPos = vd.endsPoint.getTargetStartPoint(itemPos)

            composeItemData.item.renderer.f()(composeItemData.viewData)
                    .bimap({ it.push("ComposeRenderer") })
                    { pos ->
                        RendererPos(itemRealStartPos + pos.start, pos.count)
                    }
        }
//</editor-fold>

//<editor-fold desc="SplitRenderer">
fun <T, VD : ViewData<T>> BaseRenderer<T, SplitViewData<T>>.itemPos(
        pos: Int,
        f: BaseRenderer<T, VD>.() -> PosAction<VD> = { me() }): PosAction<SplitViewData<T>> =
        result@{ vd ->
            val realRenderer = this@itemPos.fix()
            val splitItemData = vd.vdList.getOrNull(pos)
            val targetRenderer = realRenderer.renderers.getOrNull(pos)

            if (splitItemData == null || targetRenderer == null)
                return@result ErrorMsg("SplitRenderer",
                        "Not have match splitItem. splitItemData: $splitItemData; pos=$pos; itemList: ${vd.vdList.joinToString()}")
                        .left()

            val itemRealStartPos = vd.endsPoint.getTargetStartPoint(pos)

            (targetRenderer as BaseRenderer<T, VD>).f()(splitItemData as VD)
                    .bimap({ it.push("SplitRenderer") })
                    { pos ->
                        RendererPos(itemRealStartPos + pos.start, pos.count)
                    }
        }
//</editor-fold>

//<editor-fold desc="ListRenderer">
fun <T, I, IV : ViewData<I>> BaseRenderer<T, ListViewData<T, I, IV>>.subsPos(
        index: Int,
        f: BaseRenderer<I, IV>.() -> PosAction<IV> = { me() }): PosAction<ListViewData<T, I, IV>> =
        subsPosWithOriginal({ _, _ -> index }, f)

fun <T, I, IV : ViewData<I>> BaseRenderer<T, ListViewData<T, I, IV>>.subsPos(
        indexFun: (List<I>) -> Int,
        f: BaseRenderer<I, IV>.() -> PosAction<IV> = { me() }): PosAction<ListViewData<T, I, IV>> =
        subsPosWithOriginal({ _, list -> indexFun(list) }, f)

fun <T, I, IV : ViewData<I>> BaseRenderer<T, ListViewData<T, I, IV>>.subsPosWithOriginal(
        indexFun: (T, List<I>) -> Int,
        f: BaseRenderer<I, IV>.() -> PosAction<IV> = { me() }): PosAction<ListViewData<T, I, IV>> =
        result@{ vd ->
            val index = indexFun(vd.originData, vd.data)
            if (index < 0 || index >= vd.data.size)
                return@result ErrorMsg("ListRenderer",
                        "index $index out of bounds, size ${vd.data.size}.").left()

            val realRenderer = this@subsPosWithOriginal.fix()

            val itemRealStartPos = vd.endsPoint.getTargetStartPoint(index)

            realRenderer.subs.f()(vd.list[index])
                    .bimap({ it.push("ListRenderer") })
                    { pos ->
                        RendererPos(itemRealStartPos + pos.start, pos.count)
                    }
        }
//</editor-fold>

//<editor-fold desc="MapperRenderer">
fun <T, D, VD : ViewData<D>> BaseRenderer<T, MapperViewData<T, D, VD>>.mapper(
        f: BaseRenderer<D, VD>.() -> PosAction<VD> = { me() }): PosAction<MapperViewData<T, D, VD>> =
        result@{ vd ->
            val realRenderer = this@mapper.fix()
            realRenderer.targetRenderer.f()(vd.mapVD)
                    .bimap({ it.push("MapperRenderer") })
                    { pos -> pos }
        }
//</editor-fold>

//<editor-fold desc="SealedItemRenderer">

fun <T, D, VD : ViewData<D>,
        L : HListK<Kind<ForSealedItem, T>, L>> BaseRenderer<T, SealedViewData<T, L>>.sealedItem(
        sealedFun: L.() -> SealedItemOf<T, D, VD>,
        f: BaseRenderer<D, VD>.() -> PosAction<VD> = { me() }): PosAction<SealedViewData<T, L>> =
        sealedItemWithVD({ sealedFun() }, f)

fun <T, D, VD : ViewData<D>,
        L : HListK<Kind<ForSealedItem, T>, L>> BaseRenderer<T, SealedViewData<T, L>>.sealedItemWithVD(
        sealedFun: L.(T) -> SealedItemOf<T, D, VD>,
        f: BaseRenderer<D, VD>.() -> PosAction<VD> = { me() }): PosAction<SealedViewData<T, L>> =
        result@{ vd ->
            val realRenderer = this@sealedItemWithVD.fix()

            val sealedItem: SealedItem<T, D, VD> = realRenderer.sealedList.sealedFun(vd.originData).fix()
            if (sealedItem !== vd.item)
                return@result ErrorMsg("SealedItemRenderer",
                        "sealedItem is not match: sealedFun=$sealedItem; but viewData is ${vd.item}.").left()

            (sealedItem as SealedItem<T, D, VD>).renderer.f()(vd.data as VD)
                    .bimap({ it.push("SealedItemRenderer") })
                    { pos -> pos }
        }
//</editor-fold>


//<editor-fold desc="SealedItemRenderer">

fun <T, D, VD : ViewData<D>> BaseRenderer<T, CaseViewData<T>>.caseItem(
        caseFun: List<CaseItem<T, Any?, ViewData<Any?>>>.() -> CaseItem<T, D, VD>,
        f: BaseRenderer<D, VD>.() -> PosAction<VD> = { me() }): PosAction<CaseViewData<T>> =
        caseItemWithVD({ caseFun() }, f)

fun <T, D, VD : ViewData<D>> BaseRenderer<T, CaseViewData<T>>.caseItemWithVD(
        caseFun: List<CaseItem<T, Any?, ViewData<Any?>>>.(T) -> CaseItem<T, D, VD>,
        f: BaseRenderer<D, VD>.() -> PosAction<VD> = { me() }): PosAction<CaseViewData<T>> =
        result@{ vd ->
            val realRenderer = this@caseItemWithVD.fix()

            val caseItem: CaseItem<T, D, VD> = realRenderer.caseList.caseFun(vd.originData)
            if (caseItem !== vd.item)
                return@result ErrorMsg("CaseRenderer",
                        "caseItem is not match: caseFun=$caseItem; but viewData is ${vd.item}.").left()

            (caseItem as CaseItem<T, D, VD>).renderer.f()(vd.vd as VD)
                    .bimap({ it.push("CaseRenderer") })
                    { pos -> pos }
        }
//</editor-fold>


//<editor-fold desc="TitleItemRenderer">
fun <T, G, GData: ViewData<G>, I, IData: ViewData<I>>
        BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>.titlePos(
        f: BaseRenderer<G, GData>.() -> PosAction<GData> = { me() }): PosAction<TitleViewData<T, G, GData, I, IData>> =
        result@{ vd ->
            val realRenderer = this@titlePos.fix()

            realRenderer.title.f()(vd.titleItem)
                    .bimap({ it.push("TitleItemRenderer") })
                    { pos -> pos }
        }

fun <T, G, GData: ViewData<G>, I, IData: ViewData<I>>
        BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>.itemsPos(
        index: Int,
        f: BaseRenderer<I, IData>.() -> PosAction<IData> = { me() }): PosAction<TitleViewData<T, G, GData, I, IData>> =
        itemsPosWithOri({ _, _ -> index }, f)

fun <T, G, GData: ViewData<G>, I, IData: ViewData<I>>
        BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>.itemsPos(
        indexFun: (List<I>) -> Int,
        f: BaseRenderer<I, IData>.() -> PosAction<IData> = { me() }): PosAction<TitleViewData<T, G, GData, I, IData>> =
        itemsPosWithOri({ _, list -> indexFun(list) }, f)

fun <T, G, GData: ViewData<G>, I, IData: ViewData<I>>
        BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>.itemsPosWithOri(
        indexFun: (T, List<I>) -> Int,
        f: BaseRenderer<I, IData>.() -> PosAction<IData> = { me() }): PosAction<TitleViewData<T, G, GData, I, IData>> =
        result@{ vd ->
            val realRenderer = this@itemsPosWithOri.fix()

            val itemPos = indexFun(vd.originData, vd.subs)
            if (itemPos < 0 || itemPos >= vd.subs.size)
                return@result ErrorMsg("TitleItemRenderer",
                        "index $itemPos out of bounds, size ${vd.subs.size}.").left()

            val itemVD = vd.subsData[itemPos]
            val itemRealStartPos = vd.titleSize + vd.subEndPoints.getTargetStartPoint(itemPos)

            realRenderer.subs.f()(itemVD)
                    .bimap({ it.push("TitleItemRenderer") })
                    { pos -> RendererPos(itemRealStartPos + pos.start, pos.count) }
        }
//</editor-fold>
