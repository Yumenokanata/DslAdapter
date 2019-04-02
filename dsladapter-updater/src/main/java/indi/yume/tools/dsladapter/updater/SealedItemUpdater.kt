package indi.yume.tools.dsladapter.updater

import arrow.Kind
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.datatype.EmptyAction
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD

class SealedItemUpdater<T, L : HListK<Kind<ForSealedItem, T>, L>>(
        val renderer: SealedItemRenderer<T, L>)
    : Updatable<T, SealedViewData<T, L>> {
    constructor(base: BaseRenderer<T, SealedViewData<T, L>>): this(base.fix())

    fun <D, VD : ViewData<D>, UP : Updatable<D, VD>>
            sealedItem(f: L.() -> SealedItemOf<T, D, VD>, itemUpdatable: (BaseRenderer<D, VD>) -> UP, act: UP.() -> ActionU<VD>): ActionU<SealedViewData<T, L>> =
            sealedItemReduce(f, itemUpdatable) { act() }

    @Suppress("UNCHECKED_CAST")
    fun <D, VD : ViewData<D>, UP : Updatable<D, VD>>
            sealedItemReduce(f: L.() -> SealedItemOf<T, D, VD>, itemUpdatable: (BaseRenderer<D, VD>) -> UP, act: UP.(D) -> ActionU<VD>): ActionU<SealedViewData<T, L>> {
        val sealedItem = renderer.sealedList.f().fix()

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

    fun update(data: T, payload: Any? = null): ActionU<SealedViewData<T, L>> = { oldVD ->
        val newVD = renderer.getData(data)
        updateVD(oldVD, newVD, payload) to newVD
    }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<SealedViewData<T, L>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <T, L : HListK<Kind<ForSealedItem, T>, L>> BaseRenderer<T, SealedViewData<T, L>>.updater
    get() = SealedItemUpdater(this)

fun <T, L : HListK<Kind<ForSealedItem, T>, L>> updatable(renderer: BaseRenderer<T, SealedViewData<T, L>>) =
        SealedItemUpdater(renderer)
