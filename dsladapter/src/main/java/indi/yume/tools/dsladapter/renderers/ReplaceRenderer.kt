package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ReplaceRenderer<T, VD : ViewData<T>, UP : Updatable<T, VD>> private constructor(
        val renderer: BaseRenderer<T, VD, Updatable<T, VD>>,
        val update: UP
) : BaseRenderer<T, VD, UP>() {
    override val updater: UP = update

    override fun getData(content: T): VD =
            renderer.getData(content)

    override fun getItemId(data: VD, index: Int): Long =
            renderer.getItemId(data, index)

    override fun getLayoutResId(data: VD, index: Int): Int =
            renderer.getLayoutResId(data, index)

    override fun bind(data: VD, index: Int, holder: RecyclerView.ViewHolder) =
            renderer.bind(data, index, holder)

    override fun recycle(holder: RecyclerView.ViewHolder) =
            renderer.recycle(holder)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, TUP : Updatable<T, VD>>
                replaceUpdate(
                renderer: BaseRenderer<T, VD, UP>,
                reduceFun: (UP) -> TUP): ReplaceRenderer<T, VD, TUP> =
                ReplaceRenderer(renderer as BaseRenderer<T, VD, Updatable<T, VD>>,
                        reduceFun(renderer.updater))
    }
}