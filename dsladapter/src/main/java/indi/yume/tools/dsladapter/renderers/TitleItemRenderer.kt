package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.typeclass.doNotAffectOriData

/**
 * Created by xuemaotang on 2017/11/16.
 */

class TitleItemRenderer<T, G, GData: ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>>(
        val titleGetter: (T) -> G,
        val subsGetter: (T) -> List<I>,
        val title: GBR,
        val subs: IBR,

        val titleDemapper: (T, G) -> T = doNotAffectOriData(),
        val subsDemapper: (T, List<I>) -> T = doNotAffectOriData()
) : BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>() {
    constructor(
            itemType: TypeCheck<T>,
            titleGetter: (T) -> G,
            subsGetter: (T) -> List<I>,
            title: GBR,
            subs: IBR,

            titleDemapper: (T, G) -> T = doNotAffectOriData(),
            subsDemapper: (T, List<I>) -> T = doNotAffectOriData()
    ): this(titleGetter, subsGetter, title, subs, titleDemapper, subsDemapper)

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
inline fun <T, G, GData: ViewData<G>, GBR: BaseRenderer<G, GData>, I, IData: ViewData<I>, IBR: BaseRenderer<I, IData>> BaseRenderer<T, TitleViewData<T, G, GData, I, IData>>.fix()
        : TitleItemRenderer<T, G, GData, GBR, I, IData, IBR> =
        this as TitleItemRenderer<T, G, GData, GBR, I, IData, IBR>
