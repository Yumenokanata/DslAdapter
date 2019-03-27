package indi.yume.tools.adapterdatabinding.updater

import indi.yume.tools.adapterdatabinding.DataBindingRenderer
import indi.yume.tools.adapterdatabinding.DataBindingViewData
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.updateVD

fun <T, I> DataBindingRenderer<T, I>.update(newData: T, payload: Any? = null): ActionU<DataBindingViewData<T, I>> {
    val newVD = this.getData(newData)

    return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
}