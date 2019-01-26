package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.typeclass.doNotAffectOriData

/**
 * Created by xuemaotang on 2017/11/16.
 */

class TitleItemRenderer<T, G, GData: ViewData<G>, GUP: Updatable<G, GData>, I, IData: ViewData<I>, IUP: Updatable<I, IData>>(
        val titleGetter: (T) -> G,
        val subsGetter: (T) -> List<I>,
        val title: BaseRenderer<G, GData, GUP>,
        val subs: BaseRenderer<I, IData, IUP>,

        val titleDemapper: (T, G) -> T = doNotAffectOriData(),
        val subsDemapper: (T, List<I>) -> T = doNotAffectOriData()
) : BaseRenderer<T, TitleViewData<T, G, GData, I, IData>, TitleItemUpdater<T, G, GData, GUP, I, IData, IUP>>() {
    constructor(
            itemType: TypeCheck<T>,
            titleGetter: (T) -> G,
            subsGetter: (T) -> List<I>,
            title: BaseRenderer<G, GData, GUP>,
            subs: BaseRenderer<I, IData, IUP>,

            titleDemapper: (T, G) -> T = doNotAffectOriData(),
            subsDemapper: (T, List<I>) -> T = doNotAffectOriData()
    ): this(titleGetter, subsGetter, title, subs, titleDemapper, subsDemapper)

    override val updater: TitleItemUpdater<T, G, GData, GUP, I, IData, IUP> = TitleItemUpdater(this)

    override fun getData(content: T): TitleViewData<T, G, GData, I, IData> =
            TitleViewData(content,
                    title.getData(titleGetter(content)),
                    subsGetter(content).map { subs.getData(it) })

    override fun getItemId(data: TitleViewData<T, G, GData, I, IData>, index: Int): Long =
        when {
            index < data.titleSize -> title.getItemId(data.titleItem, index)
            else -> {
                val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index - data.titleSize, data.subEndPoints)
                subs.getItemId(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
            }
        }

    override fun getItemViewType(data: TitleViewData<T, G, GData, I, IData>, position: Int): Int =
            when {
                position < data.titleSize -> title.getItemViewType(data.titleItem, position)
                else -> {
                    val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position - data.titleSize, data.subEndPoints)
                    subs.getItemViewType(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
                }
            }

    override fun getLayoutResId(data: TitleViewData<T, G, GData, I, IData>, position: Int): Int =
            when {
                position < data.titleSize -> title.getLayoutResId(data.titleItem, position)
                else -> {
                    val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position - data.titleSize, data.subEndPoints)
                    subs.getLayoutResId(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
                }
            }

    override fun bind(data: TitleViewData<T, G, GData, I, IData>, index: Int, holder: RecyclerView.ViewHolder) {
        when {
            index < data.titleSize -> {
                title.bind(data.titleItem, index, holder)
                holder.bindRecycle(this) { title.recycle(it) }
            }
            else -> {
                val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index - data.titleSize, data.subEndPoints)
                subs.bind(data.subsData[resolvedRepositoryIndex], resolvedItemIndex, holder)
                holder.bindRecycle(this) { subs.recycle(it) }
            }
        }
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }
}

data class TitleViewData<T, G, GVD: ViewData<G>, I, IVD : ViewData<I>>(override val originData: T,
                                                                       val titleItem: GVD,
                                                                       val subsData: List<IVD>) : ViewData<T> {
    val titleSize: Int = titleItem.count

    val subEndPoints: IntArray = subsData.getEndsPoints()

    override val count: Int = titleSize + subEndPoints.getEndPoint()
}


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, G, GData: ViewData<G>, GUP: Updatable<G, GData>, I, IData: ViewData<I>, IUP: Updatable<I, IData>> BaseRenderer<T, TitleViewData<T, G, GData, I, IData>, TitleItemUpdater<T, G, GData, GUP, I, IData, IUP>>.fix()
        : TitleItemRenderer<T, G, GData, GUP, I, IData, IUP> =
        this as TitleItemRenderer<T, G, GData, GUP, I, IData, IUP>

class TitleItemUpdater<T, G, GData : ViewData<G>, GUP : Updatable<G, GData>, I, IData : ViewData<I>, IUP : Updatable<I, IData>>(
        val renderer: TitleItemRenderer<T, G, GData, GUP, I, IData, IUP>)
    : Updatable<T, TitleViewData<T, G, GData, I, IData>> {
    fun update(newData: T, payload: Any? = null): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val newVD = renderer.getData(newData)

        updateVD(oldVD, newVD, payload) to newVD
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }

    fun title(updater: GUP.() -> ActionU<GData>)
            : ActionU<TitleViewData<T, G, GData, I, IData>> =
            titleReduce { updater() }

    fun titleReduce(updater: GUP.(G) -> ActionU<GData>)
            : ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val (titleActions, titleVD) = renderer.title.updater.updater(oldVD.titleItem.originData)(oldVD.titleItem)

        val newOriData = renderer.titleDemapper(oldVD.originData, titleVD.originData)

        ActionComposite(0, listOf(titleActions)) to TitleViewData(newOriData, titleVD, oldVD.subsData)
    }

    fun updateTitle(newTitleData: G, payload: Any? = null): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val newTitleVD = renderer.title.getData(newTitleData)

        val newOriData = renderer.titleDemapper(oldVD.originData, newTitleVD.originData)

        updateVD(oldVD.titleItem, newTitleVD, payload) to TitleViewData(newOriData, newTitleVD, oldVD.subsData)
    }

    fun reduceTitle(f: (oldData: G) -> ChangedData<G>): ActionU<TitleViewData<T, G, GData, I, IData>> = { oldVD ->
        val (newData, payload) = f(oldVD.titleItem.originData)
        updateTitle(newData, payload)(oldVD)
    }

    fun sub(pos: Int, updater: IUP.() -> ActionU<IData>): ActionU<TitleViewData<T, G, GData, I, IData>> =
            subReduce(pos) { updater() }

    fun subReduce(pos: Int, updater: IUP.(I) -> ActionU<IData>): ActionU<TitleViewData<T, G, GData, I, IData>> = subs@{ oldVD ->
        val subsData = oldVD.subsData
        val targetItem = subsData.getOrNull(pos) ?: return@subs EmptyAction to oldVD

        val (targetActions, targetNewVD) = renderer.subs.updater.updater(targetItem.originData)(targetItem)
        val targetRealPos = oldVD.titleSize + oldVD.subEndPoints.getTargetStartPoint(pos)
        val newItemsVD = oldVD.subsData.toMutableList().apply { set(pos, targetNewVD) }.toList()
        val newItemsData = renderer.subsGetter(oldVD.originData).toMutableList().apply { set(pos, targetNewVD.originData) }.toList()

        val newOriData = renderer.subsDemapper(oldVD.originData, newItemsData)

        ActionComposite(targetRealPos, listOf(targetActions)) to TitleViewData(newOriData, oldVD.titleItem, newItemsVD)
    }
}


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, G, GData : ViewData<G>, GUP : Updatable<G, GData>, I, IData : ViewData<I>, IUP : Updatable<I, IData>>
        UpdatableOf<T, TitleViewData<T, G, GData, I, IData>>.value(): TitleItemUpdater<T, G, GData, GUP, I, IData, IUP> =
        this as TitleItemUpdater<T, G, GData, GUP, I, IData, IUP>