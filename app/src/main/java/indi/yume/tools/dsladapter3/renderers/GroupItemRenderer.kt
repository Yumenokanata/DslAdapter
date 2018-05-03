package indi.yume.tools.dsladapter3.renderers

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import indi.yume.tools.dsladapter3.datatype.*
import indi.yume.tools.dsladapter3.typeclass.BaseRenderer
import indi.yume.tools.dsladapter3.typeclass.ViewData
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by xuemaotang on 2017/11/16.
 */

class GroupItemRenderer<T, G, GData: ViewData, I, IData: ViewData>(
        val groupGetter: (T) -> G,
        val subsGetter: (T) -> List<I>,
        val group: BaseRenderer<G, GData>,
        val subs: BaseRenderer<I, IData>,
        val keyGetter: (IData) -> Any? = { it },
        val detectMoves: Boolean = false
) : BaseRenderer<T, GroupViewData<GData, IData>>() {

    override fun getData(content: T): GroupViewData<GData, IData> {
        val groupData = group.getData(groupGetter(content))
        val subsData = subsGetter(content).map { subs.getData(it) }

        val ends = subsData.getEndsPonints()

        return GroupViewData(groupData, subsData, ends)
    }

    override fun getItemId(data: GroupViewData<GData, IData>, index: Int): Long =
        when {
            index == 0 -> group.getItemId(data.titleItem, 0)
            else -> {
                val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index - 1, data.endsPoint)
                subs.getItemId(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
            }
        }

    override fun getItemViewType(data: GroupViewData<GData, IData>, position: Int): Int =
            when {
                position == 0 -> group.getItemViewType(data.titleItem, 0)
                else -> {
                    val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position - 1, data.endsPoint)
                    subs.getItemViewType(data.subsData[resolvedRepositoryIndex], resolvedItemIndex)
                }
            }

    override fun getLayoutResId(data: GroupViewData<GData, IData>, position: Int): Int =
            when {
                position == 0 -> group.getLayoutResId(data.titleItem, 0)
                else -> {
                    val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position - 1, data.endsPoint)
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
                val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index - 1, data.endsPoint)
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

                val realActions = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int = oldData.subsData.size

                    override fun getNewListSize(): Int = newData.subsData.size

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                            keyGetter(oldData.subsData[oldItemPosition]) == keyGetter(newData.subsData[oldItemPosition])

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                            oldData.subsData[oldItemPosition] == newData.subsData[oldItemPosition]
                }, detectMoves).toActionsWithRealIndex(oldData.subsData, newData.subsData)
                { act, indexData ->
                    var startIndex = 0
                    var index = 0
                    val actionComposite = ArrayList<UpdateActions>()

                    for (i in 0 until (act.pos + act.count)) {
                        if(i == act.pos)
                            startIndex = index
                        if(i >= act.pos)
                            actionComposite += ActionComposite(index,
                                    subs.getUpdates(act.oldItems!![i - act.pos], act.newItems[i - act.pos]))

                        index += indexData[i]
                    }

                    ActionComposite(startIndex, actionComposite)
                }

                yield(ActionComposite(oldData.titleItem.count, realActions))
            }.toList()
}

data class GroupViewData<G: ViewData, I: ViewData>(val titleItem: G,
                                                   val subsData: List<I>,
                                                   val endsPoint: IntArray): ViewData {
    override val count: Int = endsPoint.last() + 1
}
