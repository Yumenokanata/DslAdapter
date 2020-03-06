package indi.yume.tools.dsladapter.renderers

import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updater.LayoutUpdater
import indi.yume.tools.dsladapter.updater.Updatable

class LayoutRenderer<T>(
        @LayoutRes val layout: Int,
        val count: Int = 1,
        val bindHolder: (RecyclerView.ViewHolder, T, Int) -> Unit = { _, _, _ -> },
        val recycleHolderFun: (RecyclerView.ViewHolder) -> Unit = { },
        val stableIdForItem: (T, Int) -> Long = { _, _ -> -1L }
) : BaseRenderer<T, LayoutViewData<T>>() {
    constructor(
            @LayoutRes layout: Int,
            count: Int = 1,
            binder: (View, T, Int) -> Unit = { _, _, _ -> },
            recycleFun: (View) -> Unit = { },
            stableIdForItem: (T, Int) -> Long = { _, _ -> -1L },
            viewGetter: (RecyclerView.ViewHolder) -> View = { it.itemView }
    ): this(
            layout = layout, count = count,
            bindHolder = { holder, t, index -> binder(viewGetter(holder), t, index) },
            recycleHolderFun = { holder -> recycleFun(viewGetter(holder)) },
            stableIdForItem = stableIdForItem
    )

    override val defaultUpdater: Updatable<T, LayoutViewData<T>> = LayoutUpdater(this)

    override fun getData(content: T): LayoutViewData<T> = LayoutViewData(count, content)

    override fun getItemId(data: LayoutViewData<T>, index: Int): Long = stableIdForItem(data.originData, index)

    override fun getLayoutResId(data: LayoutViewData<T>, index: Int): Int = layout

    override fun bind(data: LayoutViewData<T>, index: Int, holder: RecyclerView.ViewHolder) {
        bindHolder(holder, data.originData, index)
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        recycleHolderFun(holder)
    }

    companion object
}

data class LayoutViewData<T>(override val count: Int, override val originData: T) : ViewData<T>


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> BaseRenderer<T, LayoutViewData<T>>.fix(): LayoutRenderer<T> =
        this as LayoutRenderer<T>

