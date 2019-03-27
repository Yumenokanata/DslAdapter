package indi.yume.tools.dsladapter.updater.ignore

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.IgnoreRenderer
import indi.yume.tools.dsladapter.renderers.IgnoreViewData

fun <T> IgnoreRenderer<T>.update(newData: T, payload: Any? = null): ActionU<IgnoreViewData<T>> =
        { oldVD ->
            val (act, realVD) = reduceFun(renderer, oldVD.originData, newData, payload)(oldVD.originVD)
            act to IgnoreViewData(realVD)
        }

fun <T> IgnoreRenderer<T>.reduce(f: (oldData: T) -> ChangedData<T>): ActionU<IgnoreViewData<T>> =
        { oldVD ->
            val (newData, payload) = f(oldVD.originData)
            update(newData, payload)(oldVD)
        }