package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.datatype.EmptyAction
import indi.yume.tools.dsladapter.renderers.EmptyRenderer
import indi.yume.tools.dsladapter.renderers.EmptyViewData
import indi.yume.tools.dsladapter.renderers.fix
import indi.yume.tools.dsladapter.typeclass.BaseRenderer

class EmptyUpdater<T>(
        val renderer: EmptyRenderer<T>
) : Updatable<T, EmptyViewData<T>> {
    constructor(base: BaseRenderer<T, EmptyViewData<T>>): this(base.fix())

    override fun autoUpdate(newData: T): ActionU<EmptyViewData<T>> = { oldVD ->
        EmptyAction to renderer.getData(newData)
    }
}


val <T> BaseRenderer<T, EmptyViewData<T>>.updater get() = EmptyUpdater(this)

fun <T> updatable(renderer: BaseRenderer<T, EmptyViewData<T>>) = EmptyUpdater(renderer)