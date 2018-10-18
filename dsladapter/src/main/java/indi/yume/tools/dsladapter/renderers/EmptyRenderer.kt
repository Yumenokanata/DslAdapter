package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

/**
 * Created by yume on 18-3-20.
 */

class EmptyRenderer<T> : BaseRenderer<T, EmptyViewData>() {
    override fun getData(content: T): EmptyViewData = EmptyViewData

    override fun getItemViewType(data: EmptyViewData, position: Int): Int =
            throw UnsupportedOperationException("EmptyRenderer do not support getItemViewType.")

    override fun getLayoutResId(data: EmptyViewData, position: Int): Int =
            throw UnsupportedOperationException("EmptyRenderer do not support getLayoutResId.")

    override fun bind(data: EmptyViewData, index: Int, holder: RecyclerView.ViewHolder) =
            throw UnsupportedOperationException("EmptyRenderer do not support bind.")

    override fun recycle(holder: RecyclerView.ViewHolder) =
            throw UnsupportedOperationException("EmptyRenderer do not support recycle.")

    override fun getUpdates(oldData: EmptyViewData, newData: EmptyViewData): List<UpdateActions> =
            emptyList()
}

object EmptyViewData : ViewData {
    override val count: Int = 0
}