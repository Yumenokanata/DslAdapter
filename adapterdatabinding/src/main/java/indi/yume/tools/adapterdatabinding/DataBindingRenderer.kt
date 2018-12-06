package indi.yume.tools.adapterdatabinding

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.datatype.toActionsWithRealIndex
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import java.util.*

class DataBindingRenderer<T, I: Any>(
        val layout: (I) -> Int,
        val converte: (T) -> List<I>,
        val itemIds: List<Pair<(I) -> Int, (I) -> Any?>>,
        val handlers: List<Pair<Int, Any?>>,
        @RecycleConfig val recycleConfig: Int = DO_NOTHING,
        val stableIdForItem: (I) -> Long,
        val collectionId: Int = BR_NO_ID,
        val keyGetter: (I, Int) -> Any? = { i, index -> i }
): BaseRenderer<T, DataBindingViewData<I>>() {
    override fun getData(content: T): DataBindingViewData<I> =
            DataBindingViewData(converte(content))

    override fun getItemId(data: DataBindingViewData<I>, index: Int): Long =
            stableIdForItem(data.data[index])

    override fun getLayoutResId(data: DataBindingViewData<I>, position: Int): Int =
            layout(data.data[position])

    override fun bind(data: DataBindingViewData<I>, index: Int, holder: RecyclerView.ViewHolder) {
        val item = data.data[index]
        val view = holder.itemView
        val viewDataBinding = DataBindingUtil.bind<ViewDataBinding>(view)!!

        val idList = lazy { LinkedList<Int>() }
        for ((idGet, setter) in itemIds) {
            val itemVariable = idGet(item)
            if (itemVariable != BR_NO_ID) {
                viewDataBinding.setVariable(itemVariable, setter(item))
                idList.value.add(itemVariable)
            }
        }
        if (idList.isInitialized())
            view.setTag(R.id.avocado__adapterdatabinding__item_id, idList.value)

        if (collectionId != BR_NO_ID) {
            viewDataBinding.setVariable(collectionId, data)
            view.setTag(R.id.avocado__adapterdatabinding__collection_id, collectionId)
        }
        for ((id, handle) in handlers) {
            viewDataBinding.setVariable(id, handle)
        }
        viewDataBinding.executePendingBindings()
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        if (recycleConfig != DO_NOTHING) {
            val view = holder.itemView
            val viewDataBinding = DataBindingUtil.bind<ViewDataBinding>(view)!!
            if (recycleConfig and CLEAR_ITEM != 0) {
                val tag = view.getTag(R.id.avocado__adapterdatabinding__item_id)
                view.setTag(R.id.avocado__adapterdatabinding__item_id, null)
                if (tag is List<*>) {
                    for (id in tag)
                        if (id is Int)
                            viewDataBinding.setVariable(id, null)
                }
            }
            if (recycleConfig and CLEAR_COLLECTION != 0) {
                val collectionTag = view.getTag(R.id.avocado__adapterdatabinding__collection_id)
                view.setTag(R.id.avocado__adapterdatabinding__collection_id, null)
                if (collectionTag is Int) {
                    viewDataBinding.setVariable(collectionTag, null)
                }
            }
            if (recycleConfig and CLEAR_HANDLERS != 0) {
                for ((id, _) in handlers)
                    viewDataBinding.setVariable(id, null)
            }
            viewDataBinding.executePendingBindings()
        }
    }

    override fun getUpdates(oldData: DataBindingViewData<I>, newData: DataBindingViewData<I>): List<UpdateActions> {
        val realActions = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldData.data.size

            override fun getNewListSize(): Int = newData.data.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    keyGetter(oldData.data[oldItemPosition], oldItemPosition) == keyGetter(newData.data[newItemPosition], newItemPosition)

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldData.data[oldItemPosition] == newData.data[newItemPosition]
        }, false).toActionsWithRealIndex(oldData.data, newData.data, null, { 1 })

        return realActions
    }

    companion object {
        internal const val BR_NO_ID = -1
    }
}


data class DataBindingViewData<T>(val data: List<T>) : ViewData {
    override val count: Int = data.size
}