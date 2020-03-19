package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class LazyUpdater<T, VD: ViewData<T>>(val renderer: LazyRenderer<T, VD>) : Updatable<T, LazyViewData<T, VD>> {
    constructor(base: BaseRenderer<T, LazyViewData<T, VD>>): this(base.fix())

    override fun autoUpdate(newData: T): ActionU<LazyViewData<T, VD>> = { oldVD ->
        val (actions, newInnerVD) = renderer.realRenderer.defaultUpdater.autoUpdate(newData)(oldVD.oriVD)

        actions to LazyViewData(newInnerVD)
    }
}

val <T, VD : ViewData<T>> BaseRenderer<T, LazyViewData<T, VD>>.updater get() = LazyUpdater(this)

fun <T, VD : ViewData<T>> updatable(renderer: BaseRenderer<T, LazyViewData<T, VD>>) = LazyUpdater(renderer.fix())