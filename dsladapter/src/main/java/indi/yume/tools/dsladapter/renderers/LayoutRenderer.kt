package indi.yume.tools.dsladapter.renderers

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.View
import indi.yume.tools.dsladapter.datatype.OnChanged
import indi.yume.tools.dsladapter.datatype.OnInserted
import indi.yume.tools.dsladapter.datatype.OnRemoved
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class LayoutRenderer<T : Any>(
        @LayoutRes val layout: Int,
        val count: Int = 1,
        val binder: (View, T, Int) -> Unit = { _, _, _ -> },
        val recycleFun: (View) -> Unit = { },
        val stableIdForItem: (T, Int) -> Long = { _, _ -> -1L },
        val keyGetter: (T) -> Any? = { it }
) : BaseRenderer<T, LayoutViewData<T>>() {

    override fun getData(content: T): LayoutViewData<T> = LayoutViewData(count, content)

    override fun getItemId(data: LayoutViewData<T>, index: Int): Long = stableIdForItem(data.data, index)

    override fun getLayoutResId(data: LayoutViewData<T>, index: Int): Int = layout

    override fun bind(data: LayoutViewData<T>, index: Int, holder: RecyclerView.ViewHolder) {
        binder(holder.itemView, data.data, index)
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        recycleFun(holder.itemView)
    }

    override fun getUpdates(oldData: LayoutViewData<T>, newData: LayoutViewData<T>): List<UpdateActions> {
        if (oldData.count != newData.count)
            throw UnknownError("oldData count is different with newData count: old=$oldData, new=$newData")

        return if (keyGetter(oldData.data) != keyGetter(newData.data))
            listOf(OnRemoved(0, newData.count),
                    OnInserted(0, newData.count))
        else
            if (oldData.data != newData.data)
                listOf(OnChanged(0, newData.count, newData.data))
            else
                emptyList()
    }

    companion object
}

data class LayoutViewData<T>(override val count: Int, val data: T) : ViewData