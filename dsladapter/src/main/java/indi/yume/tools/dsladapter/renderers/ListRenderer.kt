package indi.yume.tools.dsladapter.renderers

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

/**
 * Created by xuemaotang on 2017/11/16.
 */

class ListRenderer<T, I, IV : ViewData>(
        val converter: (T) -> List<I>,
        val subs: BaseRenderer<I, IV>,
        val keyGetter: (IV, Int) -> Any? = { iv, index -> iv }
) : BaseRenderer<T, ListViewData<IV>>() {

    override fun getData(content: T): ListViewData<IV> =
            ListViewData(converter(content).map { subs.getData(it) })

    override fun getItemId(data: ListViewData<IV>, index: Int): Long {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        return subs.getItemId(data[resolvedRepositoryIndex], resolvedItemIndex)
    }

    override fun getLayoutResId(data: ListViewData<IV>, index: Int): Int {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        return subs.getLayoutResId(data[resolvedRepositoryIndex], resolvedItemIndex)
    }

    override fun bind(data: ListViewData<IV>, index: Int, holder: RecyclerView.ViewHolder) {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        subs.bind(data[resolvedRepositoryIndex], resolvedItemIndex, holder)
        holder.itemView?.tag = Recycler { subs.recycle(it) }
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        subs.recycle(holder)
    }

    override fun getUpdates(oldData: ListViewData<IV>, newData: ListViewData<IV>): List<UpdateActions> {
        val realActions: List<UpdateActions> = if(oldData.endsPoint.last() == oldData.size
                && newData.endsPoint.last() == newData.size)
            checkListUpdates(oldData, newData, keyGetter, subs)
        else {
            val oldSize = oldData.size
            val newSize = newData.size

            oldData.withIndex().zip(newData)
            { (index, oldItem), newItem ->
                ActionComposite(
                        (if(index == 0) 0 else oldData.endsPoint[index - 1]),
                        subs.getUpdates(oldItem, newItem))
            } + when {
                oldSize == newSize -> emptyList()
                oldSize < newSize -> listOf(OnInserted(oldSize, newSize - oldSize))
                else -> listOf(OnRemoved(newSize, oldSize - newSize))
            }
        }

        return realActions
    }
}

class ListViewData<T: ViewData>(list: List<T>) : ViewData, List<T> by list {
    val endsPoint: IntArray = list.getEndsPonints()

    override val count: Int
        get() = endsPoint.last()
}