package indi.yume.tools.dsladapter.updater

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

class SealedItemUpdater<T, L : HListK<Kind<ForSealedItem, T>, L>>(
        val renderer: SealedItemRenderer<T, L>)
    : Updatable<T, SealedViewData<T>> {

    fun <D, VD : ViewData<D>, BR : BaseRenderer<D, VD>, UP : Updatable<D, VD>>
            sealedItem(f: L.() -> SealedItem<T, D, VD, BR>, itemUpdatable: (BR) -> UP, act: UP.() -> ActionU<VD>): ActionU<SealedViewData<T>> =
            sealedItemReduce(f, itemUpdatable) { act() }

    @Suppress("UNCHECKED_CAST")
    fun <D, VD : ViewData<D>, BR : BaseRenderer<D, VD>, UP : Updatable<D, VD>>
            sealedItemReduce(f: L.() -> SealedItem<T, D, VD, BR>, itemUpdatable: (BR) -> UP, act: UP.(D) -> ActionU<VD>): ActionU<SealedViewData<T>> {
        val sealedItem = renderer.sealedList.f()

        return { oldVD ->
            if (sealedItem == oldVD.item) {
                val targetVD = oldVD.data as VD
                val subAction = itemUpdatable(sealedItem.renderer).act(targetVD.originData)
//                val newData = sealedItem.mapper(oldVD.pData)
//                val (actions, subVD) = subAction(sealedItem.renderer.getData(newData))
                val (actions, subVD) = subAction(targetVD)

                val newOriData = sealedItem.demapper(oldVD.originData, subVD.originData)
                val newVD = renderer.getData(newOriData)

                actions to if (newVD.item == sealedItem) {
                    newVD.copy(data = subVD as ViewData<Any?>)
                } else newVD
            } else EmptyAction to oldVD
        }
    }

    fun update(data: T, payload: Any? = null): ActionU<SealedViewData<T>> = { oldVD ->
        val newVD = renderer.getData(data)
        updateVD(oldVD, newVD, payload) to newVD
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<SealedViewData<T>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <T, L : HListK<Kind<ForSealedItem, T>, L>> SealedItemRenderer<T, L>.updater
    get() = SealedItemUpdater(this)

fun <T, L : HListK<Kind<ForSealedItem, T>, L>> updatable(renderer: SealedItemRenderer<T, L>) =
        SealedItemUpdater(renderer)
