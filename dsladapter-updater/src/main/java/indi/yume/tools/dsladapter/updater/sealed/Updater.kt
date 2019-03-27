package indi.yume.tools.dsladapter.updater.sealed

import arrow.Kind
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.datatype.EmptyAction
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.renderers.ForSealedItem
import indi.yume.tools.dsladapter.renderers.SealedItem
import indi.yume.tools.dsladapter.renderers.SealedItemRenderer
import indi.yume.tools.dsladapter.renderers.SealedViewData
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD

fun <T, L : HListK<Kind<ForSealedItem, T>, L>,
        D, VD : ViewData<D>, BR : BaseRenderer<D, VD>>
        SealedItemRenderer<T, L>.sealedItem(f: L.() -> SealedItem<T, D, VD, BR>, act: BR.() -> ActionU<VD>): ActionU<SealedViewData<T>> =
        sealedItemReduce(f) { act() }

@Suppress("UNCHECKED_CAST")
fun <T, L : HListK<Kind<ForSealedItem, T>, L>,
        D, VD : ViewData<D>, BR : BaseRenderer<D, VD>>
        SealedItemRenderer<T, L>.sealedItemReduce(f: L.() -> SealedItem<T, D, VD, BR>, act: BR.(D) -> ActionU<VD>): ActionU<SealedViewData<T>> {
    val sealedItem = this.sealedList.f()

    return { oldVD ->
        if (sealedItem == oldVD.item) {
            val targetVD = oldVD.data as VD
            val subAction = sealedItem.renderer.act(targetVD.originData)
//                val newData = sealedItem.mapper(oldVD.pData)
//                val (actions, subVD) = subAction(sealedItem.renderer.getData(newData))
            val (actions, subVD) = subAction(targetVD)

            val newOriData = sealedItem.demapper(oldVD.originData, subVD.originData)
            val newVD = this.getData(newOriData)

            actions to if (newVD.item == sealedItem) {
                newVD.copy(data = subVD as ViewData<Any?>)
            } else newVD
        } else EmptyAction to oldVD
    }
}

fun <T, L : HListK<Kind<ForSealedItem, T>, L>>
        SealedItemRenderer<T, L>.update(data: T, payload: Any? = null): ActionU<SealedViewData<T>> = { oldVD ->
    val newVD = this.getData(data)
    updateVD(oldVD, newVD, payload) to newVD
}

fun <T, L : HListK<Kind<ForSealedItem, T>, L>>
        SealedItemRenderer<T, L>.reduce(f: (oldData: T) -> ChangedData<T>): ActionU<SealedViewData<T>> = { oldVD ->
    val (newData, payload) = f(oldVD.originData)
    update(newData, payload)(oldVD)
}