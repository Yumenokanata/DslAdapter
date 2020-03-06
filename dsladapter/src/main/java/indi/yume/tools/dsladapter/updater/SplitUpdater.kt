package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.ChangedData
import indi.yume.tools.dsladapter.datatype.ActionComposite
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.renderers.SplitRenderer
import indi.yume.tools.dsladapter.renderers.SplitViewData
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD
import java.util.*

class SplitUpdater<T>(
        val renderer: SplitRenderer<T>
): Updatable<T, SplitViewData<T>> {
    override fun autoUpdate(newData: T): ActionU<SplitViewData<T>> = { oldVD ->
        val actionUList = renderer.renderers.map { it.defaultUpdater.autoUpdate(newData) }

        var index = 0
        val actionList = LinkedList<UpdateActions>()
        val newVDList = LinkedList<ViewData<T>>()
        for ((actionU, subOldVD) in actionUList.zip(oldVD.vdList)) {
            val (subActions, subNewVD) = actionU(subOldVD)
            actionList += ActionComposite(index, subActions)
            newVDList += subNewVD
            index += subOldVD.count
        }

        ActionComposite(0, actionList.toList()) to
                SplitViewData(newData, newVDList)
    }

    @Suppress("UNCHECKED_CAST")
    fun <VD : ViewData<T>> unsafeUpdateItem(
            pos: Int,
            changeSumData: Boolean = false,
            itemUpdatable: BaseRenderer<T, VD>.(oldData: T) -> ActionU<VD>): ActionU<SplitViewData<T>> =
            { oldVD ->
                val subRenderer = renderer.renderers[pos] as BaseRenderer<T, VD>
                val subOldVD = oldVD.vdList[pos] as VD

                val (subActions, subNewVD) = subRenderer.itemUpdatable(subOldVD.originData)(subOldVD)
                val newVD = oldVD.vdList.toMutableList().apply {
                    set(pos, subNewVD)
                }.toList()

                val realTargetStartIndex = oldVD.vdList.take(pos).sumBy { it.count }

                ActionComposite(realTargetStartIndex, subActions) to
                        SplitViewData(if (changeSumData) subNewVD.originData else oldVD.originData, newVD)
            }

    fun update(data: T, payload: Any? = null): ActionU<SplitViewData<T>> =
            if (payload == null) autoUpdate(data)
            else { oldVD ->
                val newVD = renderer.getData(data)
                updateVD(oldVD, newVD, payload) to newVD
            }

    fun reduce(f: (oldData: T) -> ChangedData<T>): ActionU<SplitViewData<T>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}