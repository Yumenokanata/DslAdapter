package indi.yume.tools.dsladapter.updater.databinding

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.renderers.databinding.DataBindingRenderer
import indi.yume.tools.dsladapter.renderers.databinding.DataBindingViewData
import indi.yume.tools.dsladapter.updateVD

fun <T, I> DataBindingRenderer<T, I>.update(newData: T, payload: Any? = null): ActionU<DataBindingViewData<T, I>> {
    val newVD = this.getData(newData)

    return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
}