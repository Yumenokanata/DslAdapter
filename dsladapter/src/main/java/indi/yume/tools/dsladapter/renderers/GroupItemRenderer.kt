package indi.yume.tools.dsladapter.renderers

import android.support.v7.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by xuemaotang on 2017/11/16.
 */

class GroupItemRenderer<T, G, GData: ViewData, I, IData: ViewData>(
        val groupGetter: (T) -> G,
        val subsGetter: (T) -> List<I>,
        val group: BaseRenderer<G, GData>,
        val subs: BaseRenderer<I, IData>,
        val keyGetter: (IData, Int) -> Any? = { i, index -> i },
        val detectMoves: Boolean = false
) : BaseRenderer<T, GroupViewData<GData, IData>>() {

    override fun getData(content: T): GroupViewData<GData, IData> =
            GroupViewData(group.getData(groupGetter(content)),
                    subsGetter(content).map { subs.getData(it) })

    override fun getItemId(data: GroupViewData<GData, IData>, index: Int): Long =
        when {
            index == 0 -> group.getItemId(data.titleItem, 0)
            else -> {
                val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index - 1, data.subEndPoints)
                subs.getItemId(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
            }
        }

    override fun getItemViewType(data: GroupViewData<GData, IData>, position: Int): Int =
            when {
                position == 0 -> group.getItemViewType(data.titleItem, 0)
                else -> {
                    val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position - 1, data.subEndPoints)
                    subs.getItemViewType(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
                }
            }

    override fun getLayoutResId(data: GroupViewData<GData, IData>, position: Int): Int =
            when {
                position == 0 -> group.getLayoutResId(data.titleItem, 0)
                else -> {
                    val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position - 1, data.subEndPoints)
                    subs.getLayoutResId(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
                }
            }

    override fun bind(data: GroupViewData<GData, IData>, index: Int, holder: RecyclerView.ViewHolder) {
        when {
            index == 0 -> {
                group.bind(data.titleItem, 0, holder)
                holder.itemView?.tag = Recycler { group.recycle(it) }
            }
            else -> {
                val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index - 1, data.subEndPoints)
                subs.bind(data.subsData[resolvedRepositoryIndex], resolvedItemIndex, holder)
                holder.itemView?.tag = Recycler { subs.recycle(it) }
            }
        }
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        val tag = holder.itemView?.tag
        if(tag is Recycler)
            tag.f(holder)

        holder.itemView?.tag = null
    }

    override fun getUpdates(oldData: GroupViewData<GData, IData>, newData: GroupViewData<GData, IData>): List<UpdateActions> =
            buildSequence {
                if (oldData.titleItem != newData.titleItem) {
                    yield(OnChanged(0, 1, null))
                }

                val realActions: List<UpdateActions> = if(oldData.subEndPoints.last() == oldData.subEndPoints.size
                        && newData.subEndPoints.last() == newData.subEndPoints.size)
                    checkListUpdates(oldData.subsData, newData.subsData, keyGetter, subs)
                else {
                    val oldSize = oldData.subsData.size
                    val newSize = newData.subsData.size

                    oldData.subsData.withIndex().zip(newData.subsData)
                    { (index, oldItem), newItem ->
                        ActionComposite(
                                (if(index == 0) 0 else oldData.subEndPoints[index - 1]),
                                subs.getUpdates(oldItem, newItem))
                    } + when {
                        oldSize == newSize -> emptyList()
                        oldSize < newSize -> listOf(OnInserted(oldSize, newSize - oldSize))
                        else -> listOf(OnRemoved(newSize, oldSize - newSize))
                    }
                }

                yield(ActionComposite(oldData.titleItem.count, realActions))
            }.toList()
}

data class GroupViewData<G: ViewData, I: ViewData>(val titleItem: G,
                                                   val subsData: List<I>): ViewData {
    val subEndPoints: IntArray = subsData.getEndsPonints()

    override val count: Int = subEndPoints.last() + 1
}
