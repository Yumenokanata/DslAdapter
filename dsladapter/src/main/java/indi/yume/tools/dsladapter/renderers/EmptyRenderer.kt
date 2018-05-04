package indi.yume.tools.dsladapter.renderers

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.Renderer
import indi.yume.tools.dsladapter.typeclass.ViewData

/**
 * Created by yume on 18-3-20.
 */

class EmptyRenderer<T> : Renderer<T, EmptyViewData> {
    override fun getData(content: T): EmptyViewData = EmptyViewData

    override fun getItemViewType(data: EmptyViewData, position: Int): Int =
            throw UnsupportedOperationException("EmptyRenderer do not support getItemViewType.")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            throw UnsupportedOperationException("EmptyRenderer do not support onCreateViewHolder.")

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