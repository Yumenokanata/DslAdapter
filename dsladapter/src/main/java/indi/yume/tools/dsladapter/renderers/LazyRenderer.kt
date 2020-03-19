package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updater.LazyUpdater
import indi.yume.tools.dsladapter.updater.Updatable

class LazyRenderer<T, VD : ViewData<T>>(
        val rendererProvider: () -> BaseRenderer<T, VD>
) : BaseRenderer<T, LazyViewData<T, VD>>() {
    val realRenderer: BaseRenderer<T, VD> by lazy(rendererProvider)

    override val defaultUpdater: Updatable<T, LazyViewData<T, VD>> = LazyUpdater(this)

    override fun getData(content: T): LazyViewData<T, VD> =
            LazyViewData(realRenderer.getData(content))

    override fun getItemId(data: LazyViewData<T, VD>, index: Int): Long =
            realRenderer.getItemId(data.oriVD, index)

    override fun getLayoutResId(data: LazyViewData<T, VD>, index: Int): Int =
            realRenderer.getLayoutResId(data.oriVD, index)

    override fun bind(data: LazyViewData<T, VD>, index: Int, holder: RecyclerView.ViewHolder) =
            realRenderer.bind(data.oriVD, index, holder)

    override fun recycle(holder: RecyclerView.ViewHolder) =
            realRenderer.recycle(holder)

    companion object
}

data class LazyViewData<T, VD : ViewData<T>>(val oriVD: VD) : ViewData<T> by oriVD


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD : ViewData<T>> BaseRenderer<T, LazyViewData<T, VD>>.fix(): LazyRenderer<T, VD> =
        this as LazyRenderer<T, VD>

