package indi.yume.tools.dsladapter.renderers

import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import indi.yume.tools.dsladapter.Action
import indi.yume.tools.dsladapter.TypeCheck
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.datatype.OnChanged
import indi.yume.tools.dsladapter.datatype.OnInserted
import indi.yume.tools.dsladapter.datatype.OnRemoved
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ConstantItemRenderer<T, I>(
        val count: Int = 1,
        @LayoutRes val layout: Int,
        val data: I,
        val binder: (View, I, Int) -> Unit = { _, _, _ -> },
        val recycleFun: (View) -> Unit = { },
        val stableIdForItem: (I, Int) -> Long = { _, _ -> -1L }
) : BaseRenderer<T, ConstantViewData<T, I>, ConstantUpdater<T, I>>() {
    constructor(
            type: TypeCheck<T>,
            count: Int = 1,
            @LayoutRes layout: Int,
            data: I,
            binder: (View, I, Int) -> Unit = { _, _, _ -> },
            recycleFun: (View) -> Unit = { },
            stableIdForItem: (I, Int) -> Long = { _, _ -> -1L }
    ) : this(count, layout, data, binder, recycleFun, stableIdForItem)

    override val updater: ConstantUpdater<T, I> = ConstantUpdater(this)

    override fun getData(content: T): ConstantViewData<T, I> = ConstantViewData(content, count, data)

    override fun getItemId(data: ConstantViewData<T, I>, index: Int): Long = stableIdForItem(data.data, index)

    override fun getLayoutResId(data: ConstantViewData<T, I>, index: Int): Int = layout

    override fun bind(data: ConstantViewData<T, I>, index: Int, holder: RecyclerView.ViewHolder) {
        binder(holder.itemView, data.data, index)
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        recycleFun(holder.itemView)
    }

    companion object
}

data class ConstantViewData<T, I>(override val originData: T, override val count: Int, val data: I) : ViewData<T>

class ConstantUpdater<T, I>(
        val renderer: ConstantItemRenderer<T, I>
) : Updatable<T, ConstantViewData<T, I>> {
    fun forceUpdate(payload: Any? = null): Action<ConstantViewData<T, I>> = { oldVD ->
        OnChanged(0, oldVD.count, payload) to oldVD
    }
}