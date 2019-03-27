package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.IgnoreRenderer
import indi.yume.tools.dsladapter.renderers.IgnoreViewData

class IgnoreUpdater<T>(val renderer: IgnoreRenderer<T>) : Updatable<T, IgnoreViewData<T>> {
    fun update(newData: T, payload: Any? = null): ActionU<IgnoreViewData<T>> =
            { oldVD ->
                val (act, realVD) = renderer.reduceFun(renderer.renderer, oldVD.originData, newData, payload)(oldVD.originVD)
                act to IgnoreViewData(realVD)
            }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<IgnoreViewData<T>> =
            { oldVD ->
                val (newData, payload) = f(oldVD.originData)
                update(newData, payload)(oldVD)
            }
}

val <T> IgnoreRenderer<T>.updater get() = IgnoreUpdater(this)

fun <T> updatable(renderer: IgnoreRenderer<T>) = IgnoreUpdater(renderer)
