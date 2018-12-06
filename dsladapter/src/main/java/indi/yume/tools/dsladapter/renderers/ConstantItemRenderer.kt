package indi.yume.tools.dsladapter.renderers

import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.OnInserted
import indi.yume.tools.dsladapter.datatype.OnRemoved
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ConstantItemRenderer<T : Any, I>(
        val count: Int = 1,
        @LayoutRes val layout: Int,
        val data: I,
        val binder: (View, I, Int) -> Unit = { _, _, _ -> },
        val recycleFun: (View) -> Unit = { },
        val stableIdForItem: (I, Int) -> Long = { _, _ -> -1L }
) : BaseRenderer<T, ConstantViewData<I>>() {
    val viewData = ConstantViewData(count, data)

    override fun getData(content: T): ConstantViewData<I> = viewData

    override fun getItemId(data: ConstantViewData<I>, index: Int): Long = stableIdForItem(data.data, index)

    override fun getLayoutResId(data: ConstantViewData<I>, index: Int): Int = layout

    override fun bind(data: ConstantViewData<I>, index: Int, holder: RecyclerView.ViewHolder) {
        binder(holder.itemView, data.data, index)
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        recycleFun(holder.itemView)
    }

    override fun getUpdates(oldData: ConstantViewData<I>, newData: ConstantViewData<I>): List<UpdateActions> {
        val oldSize = oldData.count
        val newSize = newData.count

        return when {
            oldSize < newSize -> listOf(OnInserted(oldSize - 1, newSize - oldSize))
            oldSize > newSize -> listOf(OnRemoved(newSize, oldSize - newSize))
            else -> emptyList()
        }
    }

    companion object
}

data class ConstantViewData<I>(override val count: Int, val data: I) : ViewData