package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.renderers.CaseRenderer
import indi.yume.tools.dsladapter.renderers.CaseViewData
import indi.yume.tools.dsladapter.updateVD

class CaseUpdater<T>(
        val renderer: CaseRenderer<T>
) : Updatable<T, CaseViewData<T>> {

    override fun autoUpdate(newData: T): ActionU<CaseViewData<T>> = { oldVD ->
        val newVD = renderer.getData(newData)

        if (oldVD.item == newVD.item) {
            val sealedItem = newVD.item
            val actionU = sealedItem.renderer.defaultUpdater.autoUpdate(newVD.vd.originData)

            actionU(oldVD.vd).first to newVD
        } else {
            updateVD(oldVD, newVD, null) to newVD
        }
    }

    fun update(data: T, payload: Any? = null): ActionU<CaseViewData<T>> =
            if (payload == null) autoUpdate(data)
            else { oldVD ->
                val newVD = renderer.getData(data)
                updateVD(oldVD, newVD, payload) to newVD
            }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<CaseViewData<T>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}