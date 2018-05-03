package indi.yume.tools.dsladapter3.renderers

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.View
import indi.yume.tools.dsladapter3.datatype.OnInserted
import indi.yume.tools.dsladapter3.datatype.OnRemoved
import indi.yume.tools.dsladapter3.datatype.UpdateActions
import indi.yume.tools.dsladapter3.typeclass.BaseRenderer
import indi.yume.tools.dsladapter3.typeclass.ViewData

class ConstantItemRenderer<T : Any>(
        val count: Int = 1,
        @LayoutRes val layout: Int,
        val binder: (View, T, Int) -> Unit = { _, _, _ -> },
        val recycleFun: (View) -> Unit = { },
        val stableIdForItem: (T, Int) -> Long = { _, _ -> -1L }
) : BaseRenderer<T, ConstantViewData<T>>() {
    override fun getData(content: T): ConstantViewData<T> = ConstantViewData(count, content)

    override fun getItemId(data: ConstantViewData<T>, index: Int): Long = stableIdForItem(data.data, index)

    override fun getLayoutResId(data: ConstantViewData<T>, index: Int): Int = layout

    override fun bind(data: ConstantViewData<T>, index: Int, holder: RecyclerView.ViewHolder) {
        binder(holder.itemView, data.data, index)
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        recycleFun(holder.itemView)
    }

    override fun getUpdates(oldData: ConstantViewData<T>, newData: ConstantViewData<T>): List<UpdateActions> {
        val oldSize = oldData.count
        val newSize = newData.count

        return when {
            oldSize < newSize -> listOf(OnInserted(oldSize - 1, newSize - oldSize))
            oldSize > newSize -> listOf(OnRemoved(newSize, oldSize - newSize))
            else -> emptyList()
        }
    }
}

data class ConstantViewData<T>(override val count: Int, val data: T) : ViewData