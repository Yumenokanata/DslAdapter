package indi.yume.tools.dsladapter.renderers

import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class LayoutRenderer<T>(
        @LayoutRes val layout: Int,
        val count: Int = 1,
        val binder: (View, T, Int) -> Unit = { _, _, _ -> },
        val recycleFun: (View) -> Unit = { },
        val stableIdForItem: (T, Int) -> Long = { _, _ -> -1L }
) : BaseRenderer<T, LayoutViewData<T>>() {
    override fun getData(content: T): LayoutViewData<T> = LayoutViewData(count, content)

    override fun getItemId(data: LayoutViewData<T>, index: Int): Long = stableIdForItem(data.originData, index)

    override fun getLayoutResId(data: LayoutViewData<T>, index: Int): Int = layout

    override fun bind(data: LayoutViewData<T>, index: Int, holder: RecyclerView.ViewHolder) {
        binder(holder.itemView, data.originData, index)
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        recycleFun(holder.itemView)
    }

    companion object
}

data class LayoutViewData<T>(override val count: Int, override val originData: T) : ViewData<T>


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> BaseRenderer<T, LayoutViewData<T>>.fix(): LayoutRenderer<T> =
        this as LayoutRenderer<T>

