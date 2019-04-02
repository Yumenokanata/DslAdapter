package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.datatype.ActionComposite
import indi.yume.tools.dsladapter.datatype.EmptyAction
import indi.yume.tools.dsladapter.renderers.TitleItemRenderer
import indi.yume.tools.dsladapter.renderers.TitleViewData
import indi.yume.tools.dsladapter.renderers.fix
import indi.yume.tools.dsladapter.renderers.getTargetStartPoint
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD

class TitleItemUpdater<T, G, GData : ViewData<G>, I, IData : ViewData<I>>(
        val renderer: TitleItemRenderer<T, G, GData, I, IData>)
    : Updatable<T, TitleViewData<T, G, GData, I, IData>> {
    constructor(base: BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>): this(base.fix())

    fun update(newData: T, payload: Any? = null): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val newVD = renderer.getData(newData)

        updateVD(oldVD, newVD, payload) to newVD
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }

    fun <UP : Updatable<G, GData>> title(titleUpdatable: (BaseRenderer<G, GData>) -> UP, updater: UP.() -> ActionU<GData>)
            : ActionU<TitleViewData<T, G, GData, I, IData>> =
            titleReduce(titleUpdatable) { updater() }

    fun <UP : Updatable<G, GData>> titleReduce(titleUpdatable: (BaseRenderer<G, GData>) -> UP, updater: UP.(G) -> ActionU<GData>)
            : ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val (titleActions, titleVD) = titleUpdatable(renderer.title).updater(oldVD.titleItem.originData)(oldVD.titleItem)

        val newOriData = renderer.titleDemapper(oldVD.originData, titleVD.originData)

        ActionComposite(0, listOf(titleActions)) to TitleViewData(newOriData, titleVD, oldVD.subsData, oldVD.subs)
    }

    fun updateTitle(newTitleData: G, payload: Any? = null): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val newTitleVD = renderer.title.getData(newTitleData)

        val newOriData = renderer.titleDemapper(oldVD.originData, newTitleVD.originData)

        updateVD(oldVD.titleItem, newTitleVD, payload) to
                TitleViewData(newOriData, newTitleVD, oldVD.subsData, oldVD.subs)
    }

    fun reduceTitle(f: (oldData: G) -> ChangedData<G>): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val (newData, payload) = f(oldVD.titleItem.originData)
        updateTitle(newData, payload)(oldVD)
    }

    fun <UP : Updatable<I, IData>> sub(pos: Int, subUpdatable: (BaseRenderer<I, IData>) -> UP, updater: UP.() -> ActionU<IData>): ActionU<TitleViewData<T, G, GData, I, IData>> =
            subReduce(pos, subUpdatable) { updater() }

    fun <UP : Updatable<I, IData>> subReduce(pos: Int, subUpdatable: (BaseRenderer<I, IData>) -> UP, updater: UP.(I) -> ActionU<IData>): ActionU<TitleViewData<T, G, GData, I, IData>> = subs@{ oldVD ->
        val subsData = oldVD.subsData
        val targetItem = subsData.getOrNull(pos) ?: return@subs EmptyAction to oldVD

        val (targetActions, targetNewVD) = subUpdatable(renderer.subs).updater(targetItem.originData)(targetItem)
        val targetRealPos = oldVD.titleSize + oldVD.subEndPoints.getTargetStartPoint(pos)
        val newItemsVD = oldVD.subsData.toMutableList().apply { set(pos, targetNewVD) }.toList()
        val newItemsData = renderer.subsGetter(oldVD.originData).toMutableList().apply { set(pos, targetNewVD.originData) }.toList()

        val newOriData = renderer.subsDemapper(oldVD.originData, newItemsData)

        ActionComposite(targetRealPos, listOf(targetActions)) to TitleViewData(newOriData, oldVD.titleItem, newItemsVD, newItemsData)
    }
}

val <T, G, GData : ViewData<G>, I, IData : ViewData<I>>
        BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>.updater
    get() = TitleItemUpdater(this)

fun <T, G, GData : ViewData<G>, I, IData : ViewData<I>>
        updatable(renderer: BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>) = TitleItemUpdater(renderer)


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, G, GData : ViewData<G>, I, IData : ViewData<I>>
        UpdatableOf<T, TitleViewData<T, G, GData, I, IData>>.value(): TitleItemUpdater<T, G, GData, I, IData> =
        this as TitleItemUpdater<T, G, GData, I, IData>