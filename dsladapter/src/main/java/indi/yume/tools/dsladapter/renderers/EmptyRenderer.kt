package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

/**
 * Created by yume on 18-3-20.
 */

class EmptyRenderer<T> : BaseRenderer<T, EmptyViewData<T>>() {
    override fun getData(content: T): EmptyViewData<T> = EmptyViewData(content)

    override fun getItemViewType(data: EmptyViewData<T>, position: Int): Int =
            throw UnsupportedOperationException("EmptyRenderer do not support getItemViewType.")

    override fun getLayoutResId(data: EmptyViewData<T>, position: Int): Int =
            throw UnsupportedOperationException("EmptyRenderer do not support getLayoutResId.")

    override fun bind(data: EmptyViewData<T>, index: Int, holder: RecyclerView.ViewHolder) =
            throw UnsupportedOperationException("EmptyRenderer do not support bind.")

    override fun recycle(holder: RecyclerView.ViewHolder) =
            throw UnsupportedOperationException("EmptyRenderer do not support recycle.")
}

data class EmptyViewData<T>(override val originData: T) : ViewData<T> {
    override val count: Int = 0
}
