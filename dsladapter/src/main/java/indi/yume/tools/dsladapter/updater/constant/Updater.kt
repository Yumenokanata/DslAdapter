package indi.yume.tools.dsladapter.updater.constant

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.datatype.OnChanged
import indi.yume.tools.dsladapter.renderers.ConstantItemRenderer
import indi.yume.tools.dsladapter.renderers.ConstantViewData

fun <T, I> ConstantItemRenderer<T, I>
        .forceUpdate(payload: Any? = null): ActionU<ConstantViewData<T, I>> = { oldVD ->
    OnChanged(0, oldVD.count, payload) to oldVD
}