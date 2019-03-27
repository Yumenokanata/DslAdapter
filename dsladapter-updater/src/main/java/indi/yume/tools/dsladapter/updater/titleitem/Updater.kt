package indi.yume.tools.dsladapter.updater.titleitem

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.datatype.ActionComposite
import indi.yume.tools.dsladapter.datatype.EmptyAction
import indi.yume.tools.dsladapter.renderers.TitleItemRenderer
import indi.yume.tools.dsladapter.renderers.TitleViewData
import indi.yume.tools.dsladapter.renderers.getTargetStartPoint
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.update(newData: T, payload: Any? = null): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
    val newVD = this.getData(newData)

    updateVD(oldVD, newVD, payload) to newVD
}

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.reduce(f: (oldData: T) -> ChangedData<T>): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
    val (newData, payload) = f(oldVD.originData)
    update(newData, payload)(oldVD)
}

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.title(updater: GBR.() -> ActionU<GData>)
        : ActionU<TitleViewData<T, G, GData, I, IData>> =
        titleReduce { updater() }

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.titleReduce(updater: GBR.(G) -> ActionU<GData>)
        : ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
    val (titleActions, titleVD) = this.title.updater(oldVD.titleItem.originData)(oldVD.titleItem)

    val newOriData = this.titleDemapper(oldVD.originData, titleVD.originData)

    ActionComposite(0, listOf(titleActions)) to TitleViewData(newOriData, titleVD, oldVD.subsData)
}

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.updateTitle(newTitleData: G, payload: Any? = null): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
    val newTitleVD = this.title.getData(newTitleData)

    val newOriData = this.titleDemapper(oldVD.originData, newTitleVD.originData)

    updateVD(oldVD.titleItem, newTitleVD, payload) to TitleViewData(newOriData, newTitleVD, oldVD.subsData)
}

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.reduceTitle(f: (oldData: G) -> ChangedData<G>): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
    val (newData, payload) = f(oldVD.titleItem.originData)
    updateTitle(newData, payload)(oldVD)
}

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.sub(pos: Int, updater: IBR.() -> ActionU<IData>): ActionU<TitleViewData<T, G, GData, I, IData>> =
        subReduce(pos) { updater() }

fun <T, G, GData : ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>
        TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>.subReduce(pos: Int, updater: IBR.(I) -> ActionU<IData>): ActionU<TitleViewData<T, G, GData, I, IData>> = subs@{ oldVD ->
    val subsData = oldVD.subsData
    val targetItem = subsData.getOrNull(pos) ?: return@subs EmptyAction to oldVD

    val (targetActions, targetNewVD) = this.subs.updater(targetItem.originData)(targetItem)
    val targetRealPos = oldVD.titleSize + oldVD.subEndPoints.getTargetStartPoint(pos)
    val newItemsVD = oldVD.subsData.toMutableList().apply { set(pos, targetNewVD) }.toList()
    val newItemsData = this.subsGetter(oldVD.originData).toMutableList().apply { set(pos, targetNewVD.originData) }.toList()

    val newOriData = this.subsDemapper(oldVD.originData, newItemsData)

    ActionComposite(targetRealPos, listOf(targetActions)) to TitleViewData(newOriData, oldVD.titleItem, newItemsVD)
}