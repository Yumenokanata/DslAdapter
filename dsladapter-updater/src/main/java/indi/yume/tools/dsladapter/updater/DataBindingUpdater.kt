package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.renderers.databinding.DataBindingRenderer
import indi.yume.tools.dsladapter.renderers.databinding.DataBindingViewData
import indi.yume.tools.dsladapter.renderers.databinding.fix
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.updateVD

class DataBindingUpdater<T, I>(val renderer: DataBindingRenderer<T, I>) : Updatable<T, DataBindingViewData<T, I>> {
    constructor(base: BaseRenderer<T, DataBindingViewData<T, I>>): this(base.fix())

    fun update(newData: T, payload: Any? = null): ActionU<DataBindingViewData<T, I>> {
        val newVD = renderer.getData(newData)

        return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
    }
}

val <T, I> BaseRenderer<T, DataBindingViewData<T, I>>.updater get() = DataBindingUpdater(this)

fun <T, I> updatable(renderer: BaseRenderer<T, DataBindingViewData<T, I>>) = DataBindingUpdater(renderer)


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, I> UpdatableOf<T, DataBindingViewData<T, I>>.value(): DataBindingUpdater<T, I> =
        this as DataBindingUpdater<T, I>