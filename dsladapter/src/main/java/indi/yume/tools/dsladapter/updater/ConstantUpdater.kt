package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.datatype.OnChanged
import indi.yume.tools.dsladapter.renderers.ConstantItemRenderer
import indi.yume.tools.dsladapter.renderers.ConstantViewData
import indi.yume.tools.dsladapter.renderers.fix
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.updateVD

class ConstantUpdater<T, I>(
        val renderer: ConstantItemRenderer<T, I>
) : Updatable<T, ConstantViewData<T, I>> {
    constructor(base: BaseRenderer<T, ConstantViewData<T, I>>): this(base.fix())

    fun forceUpdate(payload: Any? = null): ActionU<ConstantViewData<T, I>> = { oldVD ->
        OnChanged(0, oldVD.count, payload) to oldVD
    }

    override fun autoUpdate(newData: T): ActionU<ConstantViewData<T, I>> = { oldVD ->
        val newVD = renderer.getData(newData)

        updateVD(oldVD, newVD, null) to newVD
    }
}

val <T, I> BaseRenderer<T, ConstantViewData<T, I>>.updater get() = ConstantUpdater(this)

fun <T, I> updatable(renderer: BaseRenderer<T, ConstantViewData<T, I>>) = ConstantUpdater(renderer)

