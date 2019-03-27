package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class IgnoreRenderer<T> private constructor(
        val renderer: BaseRenderer<T, ViewData<T>>,
        val reduceFun: BaseRenderer<T, ViewData<T>>.(oldData: T, newData: T, payload: Any?) -> ActionU<ViewData<T>>
) : BaseRenderer<T, IgnoreViewData<T>>() {
    override fun getData(content: T): IgnoreViewData<T> =
            IgnoreViewData(renderer.getData(content))

    override fun getItemId(data: IgnoreViewData<T>, index: Int): Long =
            renderer.getItemId(data.originVD, index)

    override fun getLayoutResId(data: IgnoreViewData<T>, index: Int): Int =
            renderer.getLayoutResId(data.originVD, index)

    override fun bind(data: IgnoreViewData<T>, index: Int, holder: RecyclerView.ViewHolder) =
            renderer.bind(data.originVD, index, holder)

    override fun recycle(holder: RecyclerView.ViewHolder) =
            renderer.recycle(holder)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>> ignoreType(
                renderer: BR,
                reduceFun: BR.(oldData: T, newData: T, payload: Any?) -> ActionU<VD>): IgnoreRenderer<T> =
                IgnoreRenderer(renderer as BaseRenderer<T, ViewData<T>>,
                        reduceFun as BaseRenderer<T, ViewData<T>>.(oldData: T, newData: T, payload: Any?) -> ActionU<ViewData<T>>)
    }
}

data class IgnoreViewData<T>(val originVD: ViewData<T>) : ViewData<T> {
    override val count: Int = originVD.count
    override val originData: T = originVD.originData
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> BaseRenderer<T, IgnoreViewData<T>>.fix(): IgnoreRenderer<T> =
        this as IgnoreRenderer<T>
