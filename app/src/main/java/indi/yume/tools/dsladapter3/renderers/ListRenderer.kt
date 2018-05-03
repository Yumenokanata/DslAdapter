package indi.yume.tools.dsladapter3.renderers

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import indi.yume.tools.dsladapter3.datatype.ActionComposite
import indi.yume.tools.dsladapter3.datatype.UpdateActions
import indi.yume.tools.dsladapter3.datatype.toActionsWithRealIndex
import indi.yume.tools.dsladapter3.typeclass.BaseRenderer
import indi.yume.tools.dsladapter3.typeclass.ViewData

/**
 * Created by xuemaotang on 2017/11/16.
 */

class ListRenderer<T, I, IV : ViewData>(
        val converter: (T) -> List<I>,
        val subs: BaseRenderer<I, IV>,
        val keyGetter: (IV) -> Any? = { it }
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
        val realActions = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldData.size

            override fun getNewListSize(): Int = newData.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    keyGetter(oldData[oldItemPosition]) == keyGetter(newData[oldItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldData[oldItemPosition] == newData[oldItemPosition]
        }, false).toActionsWithRealIndex(oldData, newData)
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

        return realActions
    }
}

class ListViewData<T: ViewData>(list: List<T>) : ViewData, List<T> by list {
    val endsPoint: IntArray = list.getEndsPonints()

    override val count: Int
        get() = endsPoint.last()
}