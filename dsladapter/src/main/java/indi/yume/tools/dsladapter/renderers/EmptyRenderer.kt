package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.Renderer
import indi.yume.tools.dsladapter.typeclass.ViewData

/**
 * Created by yume on 18-3-20.
 */

class EmptyRenderer<T> : BaseRenderer<T, EmptyViewData<T>, EmptyUpdater<T>>() {
    override val updater: EmptyUpdater<T> = EmptyUpdater()

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

class EmptyUpdater<T> : Updatable<T, EmptyViewData<T>>