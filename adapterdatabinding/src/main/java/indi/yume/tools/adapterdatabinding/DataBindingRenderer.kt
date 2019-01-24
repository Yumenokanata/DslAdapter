package indi.yume.tools.adapterdatabinding

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.UpdatableOf
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD
import java.util.*

class DataBindingRenderer<T, I>(
        val layout: (I) -> Int,
        val converte: (T) -> List<I>,
        val itemIds: List<Pair<(I) -> Int, (I) -> Any?>>,
        val handlers: List<Pair<Int, Any?>>,
        @RecycleConfig val recycleConfig: Int = DO_NOTHING,
        val stableIdForItem: (I) -> Long,
        val collectionId: Int = BR_NO_ID
): BaseRenderer<T, DataBindingViewData<T, I>, DataBindingUpdater<T, I>>() {
    override val updater: DataBindingUpdater<T, I> = DataBindingUpdater(this)

    override fun getData(content: T): DataBindingViewData<T, I> =
            DataBindingViewData(content, converte(content))

    override fun getItemId(data: DataBindingViewData<T, I>, index: Int): Long =
            stableIdForItem(data.data[index])

    override fun getLayoutResId(data: DataBindingViewData<T, I>, position: Int): Int =
            layout(data.data[position])

    override fun bind(data: DataBindingViewData<T, I>, index: Int, holder: RecyclerView.ViewHolder) {
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

    companion object {
        internal const val BR_NO_ID = -1
    }
}


data class DataBindingViewData<T, I>(override val originData: T, val data: List<I>) : ViewData<T> {
    override val count: Int = data.size
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, I> BaseRenderer<T, DataBindingViewData<T, I>, DataBindingUpdater<T, I>>.fix(): DataBindingRenderer<T, I> =
        this as DataBindingRenderer<T, I>

class DataBindingUpdater<T, I>(val renderer: DataBindingRenderer<T, I>) : Updatable<T, DataBindingViewData<T, I>> {
    fun update(newData: T, payload: Any? = null): ActionU<DataBindingViewData<T, I>> {
        val newVD = renderer.getData(newData)

        return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
    }
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, I> UpdatableOf<T, DataBindingViewData<T, I>>.value(): DataBindingUpdater<T, I> =
        this as DataBindingUpdater<T, I>