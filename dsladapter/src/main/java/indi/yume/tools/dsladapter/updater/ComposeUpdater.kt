package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


class ComposeUpdater<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        val renderer: ComposeRenderer<DL, VDL>)
    : Updatable<DL, ComposeViewData<DL, VDL>> {
    constructor(base: BaseRenderer<DL, ComposeViewData<DL, VDL>>): this(base.fix())

    inner class ComposeGetter<T, VD : ViewData<T>>(
            val get: (VDL) -> ComposeItem<T, VD>,
            val put: (DL, T) -> DL,
            val putVD: (VDL, VD) -> VDL) {
        fun <UP : Updatable<T, VD>> up(itemUpdatable: (BaseRenderer<T, VD>) -> UP, act: UP.() -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this, itemUpdatable) { act() }

        fun up(itemUpdatable: BaseRenderer<T, VD>.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this, itemUpdatable)

        fun <UP : Updatable<T, VD>> reduce(itemUpdatable: (BaseRenderer<T, VD>) -> UP, act: UP.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this, itemUpdatable, act)

        fun reduce(itemUpdatable: BaseRenderer<T, VD>.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this, itemUpdatable)
    }

    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
            updateItem(getter: ComposeGetter<T, VD>, itemUpdatable: (BaseRenderer<T, VD>) -> UP, act: UP.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
            updateItem(getter, updateFun(itemUpdatable, act))

    @Suppress("UNCHECKED_CAST")
    fun <T, VD : ViewData<T>>
            updateItem(getter: ComposeGetter<T, VD>, itemUpdatable: BaseRenderer<T, VD>.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> = result@{ oldVD ->
        val composeItem = getter.get(oldVD.vdList)

        val (index, target) = oldVD.vdNormalList.asSequence()
                .withIndex()
                .find { it.value.item.key == composeItem.key }
                ?: return@result EmptyAction to oldVD

        val targetVD = target.viewData as VD

        val resultAction = itemUpdatable(composeItem.renderer, targetVD.originData)

        val (actions, subVD) = resultAction(targetVD)
        val newDL = getter.put(oldVD.originData, subVD.originData)
        val newVDL = getter.putVD(oldVD.vdList, subVD)

        val realTargetStartIndex = oldVD.vdNormalList.take(index).sumBy { it.viewData.count }

        ActionComposite(realTargetStartIndex, listOf(actions)) to ComposeViewData(newDL, newVDL)
    }

    fun updateBy(act: ComposeUpdater<DL, VDL>.() -> ActionU<ComposeViewData<DL, VDL>>): ActionU<ComposeViewData<DL, VDL>> =
            act()

    override fun autoUpdate(newData: DL): ActionU<ComposeViewData<DL, VDL>> = { oldVD ->
        val newVD = renderer.getData(newData)

        val vdPairList =
                oldVD.vdNormalList.zip(newVD.vdNormalList)
        val sumActions = sequence<UpdateActions> {
            var index = 0
            for ((oldItem, newItem) in vdPairList) {
                val subActionU = newItem.item.renderer.defaultUpdater.autoUpdate(newItem.viewData.originData)
                yield(ActionComposite(index,
                        listOf(subActionU(oldItem.viewData).first)))
                index += oldItem.viewData.count
            }
        }.toList()

        ActionComposite(0, sumActions) to newVD
    }

    fun update(data: DL, payload: Any? = null): ActionU<ComposeViewData<DL, VDL>> =
            if (payload == null) autoUpdate(data)
            else { oldVD ->
                val newVD = renderer.getData(data)
                updateVD(oldVD, newVD, payload) to newVD
            }

    fun reduce(f: (oldData: DL) -> ChangedData<DL>): ActionU<ComposeViewData<DL, VDL>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>
        BaseRenderer<DL, ComposeViewData<DL, VDL>>.updater get() = ComposeUpdater(this)

fun <DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>
        updatable(renderer: BaseRenderer<DL, ComposeViewData<DL, VDL>>) = ComposeUpdater(renderer)


//<editor-fold defaultstate="collapsed" desc="Updater getter">
/**
 * Mustache:
 *
 * data: {"index": 9, "sum": 10, "list":["0", "1", "2", "3", "4", "5", "6", "7", "8"]}
 *
 * template:
 * fun <{{#list}}T{{.}}, VD{{.}} : ViewData<T{{.}}>,
 *         {{/list}}
 *         T, VD : ViewData<T>
 *         DL : HListK<ForIdT, DL>,
 *         IL : HListK<ForComposeItem, IL>,
 *         VDL : HListK<ForComposeItemData, VDL>>
 *         ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, VD{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, VD>, IL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, VD{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, VD>, VDL>{{#list}}>{{/list}}>.getLast{{index}}()
 *         : ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, VD{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, VD>, IL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, VD{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, VD>, VDL>{{#list}}>{{/list}}>.ComposeGetter<T, VD> =
 *         ComposeGetter(get = { it.get{{sum}}() },
 *                 put = { dl, t -> dl.map{{sum}} { IdT(t) } },
 *                 putVD = { vdl, vd -> vdl.map{{sum}} { it.fix().copy(viewData = vd) } })
 */

fun <T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T, DL>,
                HConsK<ForComposeItemData, Pair<T, VD>, VDL>>.getLast0()
        : ComposeUpdater<HConsK<ForIdT, T, DL>,
        HConsK<ForComposeItemData, Pair<T, VD>, VDL>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get0().fix().item },
                put = { dl, t -> dl.map0 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map0 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>.getLast1()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get1().fix().item },
                put = { dl, t -> dl.map1 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map1 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T, DL>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>.getLast2()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T, DL>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get2().fix().item },
                put = { dl, t -> dl.map2 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map2 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T2, VD2 : ViewData<T2>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>.getLast3()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get3().fix().item },
                put = { dl, t -> dl.map3 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map3 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T2, VD2 : ViewData<T2>,
        T3, VD3 : ViewData<T3>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>.getLast4()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get4().fix().item },
                put = { dl, t -> dl.map4 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map4 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T2, VD2 : ViewData<T2>,
        T3, VD3 : ViewData<T3>,
        T4, VD4 : ViewData<T4>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>.getLast5()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get5().fix().item },
                put = { dl, t -> dl.map5 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map5 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T2, VD2 : ViewData<T2>,
        T3, VD3 : ViewData<T3>,
        T4, VD4 : ViewData<T4>,
        T5, VD5 : ViewData<T5>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>.getLast6()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get6().fix().item },
                put = { dl, t -> dl.map6 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map6 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T2, VD2 : ViewData<T2>,
        T3, VD3 : ViewData<T3>,
        T4, VD4 : ViewData<T4>,
        T5, VD5 : ViewData<T5>,
        T6, VD6 : ViewData<T6>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T6, VD6>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>>.getLast7()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T6, VD6>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get7().fix().item },
                put = { dl, t -> dl.map7 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map7 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T2, VD2 : ViewData<T2>,
        T3, VD3 : ViewData<T3>,
        T4, VD4 : ViewData<T4>,
        T5, VD5 : ViewData<T5>,
        T6, VD6 : ViewData<T6>,
        T7, VD7 : ViewData<T7>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T6, VD6>, HConsK<ForComposeItemData, Pair<T7, VD7>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>>>.getLast8()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T6, VD6>, HConsK<ForComposeItemData, Pair<T7, VD7>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get8().fix().item },
                put = { dl, t -> dl.map8 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map8 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>,
        T1, VD1 : ViewData<T1>,
        T2, VD2 : ViewData<T2>,
        T3, VD3 : ViewData<T3>,
        T4, VD4 : ViewData<T4>,
        T5, VD5 : ViewData<T5>,
        T6, VD6 : ViewData<T6>,
        T7, VD7 : ViewData<T7>,
        T8, VD8 : ViewData<T8>,
        T, VD : ViewData<T>,
        DL : HListK<ForIdT, DL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T8, HConsK<ForIdT, T, DL>>>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T6, VD6>, HConsK<ForComposeItemData, Pair<T7, VD7>, HConsK<ForComposeItemData, Pair<T8, VD8>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>>>>.getLast9()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T8, HConsK<ForIdT, T, DL>>>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, VD0>, HConsK<ForComposeItemData, Pair<T1, VD1>, HConsK<ForComposeItemData, Pair<T2, VD2>, HConsK<ForComposeItemData, Pair<T3, VD3>, HConsK<ForComposeItemData, Pair<T4, VD4>, HConsK<ForComposeItemData, Pair<T5, VD5>, HConsK<ForComposeItemData, Pair<T6, VD6>, HConsK<ForComposeItemData, Pair<T7, VD7>, HConsK<ForComposeItemData, Pair<T8, VD8>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>>>>>>>>>>>.ComposeGetter<T, VD> =
        ComposeGetter(get = { it.get9().fix().item },
                put = { dl, t -> dl.map9 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map9 { it.fix().copy(viewData = vd) } })
//</editor-fold>