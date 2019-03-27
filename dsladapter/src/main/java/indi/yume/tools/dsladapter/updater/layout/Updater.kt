package indi.yume.tools.dsladapter.updater.layout

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
import indi.yume.tools.dsladapter.renderers.LayoutViewData
import indi.yume.tools.dsladapter.updateVD


fun <T> LayoutRenderer<T>.update(newData: T, payload: Any? = null): ActionU<LayoutViewData<T>> {
    val newVD = this.getData(newData)

    return { oldVD -> updateVD(oldVD, newVD, payload) to newVD }
}

fun <T> LayoutRenderer<T>.reduce(f: (oldData: T) -> ChangedData<T>): ActionU<LayoutViewData<T>> = { oldVD ->
    val (newData, payload) = f(oldVD.originData)
    update(newData, payload)(oldVD)
}