package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.datatype.OnChanged
import indi.yume.tools.dsladapter.renderers.ConstantItemRenderer
import indi.yume.tools.dsladapter.renderers.ConstantViewData

class ConstantUpdater<T, I>(
        val renderer: ConstantItemRenderer<T, I>
) : Updatable<T, ConstantViewData<T, I>> {
    fun forceUpdate(payload: Any? = null): ActionU<ConstantViewData<T, I>> = { oldVD ->
        OnChanged(0, oldVD.count, payload) to oldVD
    }
}

val <T, I> ConstantItemRenderer<T, I>.updater get() = ConstantUpdater(this)

fun <T, I> updatable(renderer: ConstantItemRenderer<T, I>) = ConstantUpdater(renderer)

