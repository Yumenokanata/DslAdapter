package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD

class IgnoreRenderer<T> private constructor(
        val renderer: BaseRenderer<T, ViewData<T>, Updatable<T, ViewData<T>>>,
        val reduceFun: Updatable<T, ViewData<T>>.(oldData: T, newData: T, payload: Any?) -> ActionU<ViewData<T>>
) : BaseRenderer<T, IgnoreViewData<T>, IgnoreUpdater<T>>() {
    override val updater: IgnoreUpdater<T> = IgnoreUpdater(this)

    override fun getData(content: T): IgnoreViewData<T> =
            IgnoreViewData(renderer.getData(content))

    override fun getItemId(data: IgnoreViewData<T>, index: Int): Long =
            renderer.getItemId(data, index)

    override fun getLayoutResId(data: IgnoreViewData<T>, index: Int): Int =
            renderer.getLayoutResId(data, index)

    override fun bind(data: IgnoreViewData<T>, index: Int, holder: RecyclerView.ViewHolder) =
            renderer.bind(data, index, holder)

    override fun recycle(holder: RecyclerView.ViewHolder) =
            renderer.recycle(holder)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>> ignoreType(
                renderer: BR,
                reduceFun: UP.(oldData: T, newData: T, payload: Any?) -> ActionU<VD>): IgnoreRenderer<T> =
                IgnoreRenderer(renderer as BaseRenderer<T, ViewData<T>, Updatable<T, ViewData<T>>>,
                        reduceFun as Updatable<T, ViewData<T>>.(oldData: T, newData: T, payload: Any?) -> ActionU<ViewData<T>>)
    }
}

data class IgnoreViewData<T>(val originVD: ViewData<T>) : ViewData<T> {
    override val count: Int = originVD.count
    override val originData: T = originVD.originData
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> BaseRenderer<T, IgnoreViewData<T>, IgnoreUpdater<T>>.fix(): IgnoreRenderer<T> =
        this as IgnoreRenderer<T>

class IgnoreUpdater<T>(val renderer: IgnoreRenderer<T>) : Updatable<T, IgnoreViewData<T>> {
    fun update(newData: T, payload: Any? = null): ActionU<IgnoreViewData<T>> =
            { oldVD ->
                val (act, realVD) = renderer.reduceFun(renderer.renderer.updater, oldVD.originData, newData, payload)(oldVD.originVD)
                act to IgnoreViewData(realVD)
            }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<IgnoreViewData<T>> =
            { oldVD ->
                val (newData, payload) = f(oldVD.originData)
                update(newData, payload)(oldVD)
            }
}