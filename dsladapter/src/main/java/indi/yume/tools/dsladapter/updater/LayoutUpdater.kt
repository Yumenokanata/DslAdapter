package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
import indi.yume.tools.dsladapter.renderers.LayoutViewData
import indi.yume.tools.dsladapter.renderers.fix
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.updateVD


class LayoutUpdater<T>(val renderer: LayoutRenderer<T>) : Updatable<T, LayoutViewData<T>> {
    constructor(base: BaseRenderer<T, LayoutViewData<T>>): this(base.fix())

    override fun autoUpdate(newData: T): ActionU<LayoutViewData<T>> =
            update(newData)

    fun update(newData: T, payload: Any? = null): ActionU<LayoutViewData<T>> {
        val newVD = renderer.getData(newData)

        return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<LayoutViewData<T>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <T> BaseRenderer<T, LayoutViewData<T>>.updater get() = LayoutUpdater(this)

fun <T> updatable(renderer: BaseRenderer<T, LayoutViewData<T>>) = LayoutUpdater(renderer.fix())
