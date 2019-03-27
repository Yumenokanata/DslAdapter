package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
import indi.yume.tools.dsladapter.renderers.LayoutViewData
import indi.yume.tools.dsladapter.updateVD


class LayoutUpdater<T>(val renderer: LayoutRenderer<T>) : Updatable<T, LayoutViewData<T>> {
    fun update(newData: T, payload: Any? = null): ActionU<LayoutViewData<T>> {
        val newVD = renderer.getData(newData)

        return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<LayoutViewData<T>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <T> LayoutRenderer<T>.updater get() = LayoutUpdater(this)

fun <T> updatable(renderer: LayoutRenderer<T>) = LayoutUpdater(renderer)
