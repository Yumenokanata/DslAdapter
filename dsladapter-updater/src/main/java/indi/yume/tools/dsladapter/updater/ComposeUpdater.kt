package indi.yume.tools.dsladapter.updater

import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


class ComposeUpdater<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val renderer: ComposeRenderer<DL, IL, VDL>)
    : Updatable<DL, ComposeViewData<DL, VDL>> {

    inner class ComposeGetter<T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>(
            val get: (IL) -> ComposeOf<T, BR>,
            val put: (DL, T) -> DL,
            val putVD: (VDL, VD) -> VDL) {
        fun <UP : Updatable<T, VD>> up(itemUpdatable: (BR) -> UP, act: UP.() -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this, itemUpdatable) { act() }

        fun <UP : Updatable<T, VD>> reduce(itemUpdatable: (BR) -> UP, act: UP.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this, itemUpdatable, act)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>, UP : Updatable<T, VD>>
            updateItem(getter: ComposeGetter<T, VD, BR>, itemUpdatable: (BR) -> UP, act: UP.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> {
        val composeItem = getter.get(renderer.composeList).fix()

        return result@{ oldVD ->
            val (index, target) = oldVD.vdNormalList.asSequence()
                    .withIndex()
                    .find { it.value.item.key == composeItem.key }
                    ?: return@result EmptyAction to oldVD

            val targetVD = target.viewData as VD

            val resultAction = itemUpdatable(composeItem.renderer).act(targetVD.originData)

            val (actions, subVD) = resultAction(targetVD)
            val newDL = getter.put(oldVD.originData, subVD.originData)
            val newVDL = getter.putVD(oldVD.vdList, subVD)

            val realTargetStartIndex = oldVD.vdNormalList.take(index).sumBy { it.viewData.count }

            ActionComposite(realTargetStartIndex, listOf(actions)) to ComposeViewData(newDL, newVDL)
        }
    }

    fun updateBy(act: ComposeUpdater<DL, IL, VDL>.() -> ActionU<ComposeViewData<DL, VDL>>): ActionU<ComposeViewData<DL, VDL>> =
            act()

    fun update(data: DL, payload: Any? = null): ActionU<ComposeViewData<DL, VDL>> = { oldVD ->
        val newVD = renderer.getData(data)
        updateVD(oldVD, newVD, payload) to newVD
    }

    fun reduce(f: (oldData: DL) -> ChangedData<DL>): ActionU<ComposeViewData<DL, VDL>> = { oldVD ->
        val (newData, payload) = f(oldVD.originData)
        update(newData, payload)(oldVD)
    }
}

val <DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>
        ComposeRenderer<DL, IL, VDL>.updater get() = ComposeUpdater(this)

fun <DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>
        updatable(renderer: ComposeRenderer<DL, IL, VDL>) = ComposeUpdater(renderer)


//<editor-fold defaultstate="collapsed" desc="Updater getter">
/**
 * Mustache:
 *
 * data: {"index": 9, "sum": 10, "list":["0", "1", "2", "3", "4", "5", "6", "7", "8"]}
 *
 * template:
 * fun <{{#list}}T{{.}}, VD{{.}} : ViewData<T{{.}}>, BR{{.}} : BaseRenderer<T{{.}}, VD{{.}}>,
 *         {{/list}}
 *         T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
 *         DL : HListK<ForIdT, DL>,
 *         IL : HListK<ForComposeItem, IL>,
 *         VDL : HListK<ForComposeItemData, VDL>>
 *         ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, BR>, IL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, BR>, VDL>{{#list}}>{{/list}}>.getLast{{index}}()
 *         : ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, BR>, IL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, BR>, VDL>{{#list}}>{{/list}}>.ComposeGetter<T, VD, BR> =
 *         ComposeGetter(get = { it.get{{sum}}() },
 *                 put = { dl, t -> dl.map{{sum}} { IdT(t) } },
 *                 putVD = { vdl, vd -> vdl.map{{sum}} { it.fix().copy(viewData = vd) } })
 */

fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T, DL>,
                HConsK<ForComposeItem, Pair<T, BR>, IL>,
                HConsK<ForComposeItemData, Pair<T, BR>, VDL>>.getLast0()
        : ComposeUpdater<HConsK<ForIdT, T, DL>,
        HConsK<ForComposeItem, Pair<T, BR>, IL>,
        HConsK<ForComposeItemData, Pair<T, BR>, VDL>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get0() },
                put = { dl, t -> dl.map0 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map0 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T, BR>, IL>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>.getLast1()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T, BR>, IL>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get1() },
                put = { dl, t -> dl.map1 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map1 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T, DL>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>.getLast2()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T, DL>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get2() },
                put = { dl, t -> dl.map2 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map2 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T2, VD2 : ViewData<T2>, BR2 : BaseRenderer<T2, VD2>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>.getLast3()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get3() },
                put = { dl, t -> dl.map3 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map3 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T2, VD2 : ViewData<T2>, BR2 : BaseRenderer<T2, VD2>,
        T3, VD3 : ViewData<T3>, BR3 : BaseRenderer<T3, VD3>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>.getLast4()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get4() },
                put = { dl, t -> dl.map4 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map4 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T2, VD2 : ViewData<T2>, BR2 : BaseRenderer<T2, VD2>,
        T3, VD3 : ViewData<T3>, BR3 : BaseRenderer<T3, VD3>,
        T4, VD4 : ViewData<T4>, BR4 : BaseRenderer<T4, VD4>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>.getLast5()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get5() },
                put = { dl, t -> dl.map5 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map5 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T2, VD2 : ViewData<T2>, BR2 : BaseRenderer<T2, VD2>,
        T3, VD3 : ViewData<T3>, BR3 : BaseRenderer<T3, VD3>,
        T4, VD4 : ViewData<T4>, BR4 : BaseRenderer<T4, VD4>,
        T5, VD5 : ViewData<T5>, BR5 : BaseRenderer<T5, VD5>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>.getLast6()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get6() },
                put = { dl, t -> dl.map6 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map6 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T2, VD2 : ViewData<T2>, BR2 : BaseRenderer<T2, VD2>,
        T3, VD3 : ViewData<T3>, BR3 : BaseRenderer<T3, VD3>,
        T4, VD4 : ViewData<T4>, BR4 : BaseRenderer<T4, VD4>,
        T5, VD5 : ViewData<T5>, BR5 : BaseRenderer<T5, VD5>,
        T6, VD6 : ViewData<T6>, BR6 : BaseRenderer<T6, VD6>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>.getLast7()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get7() },
                put = { dl, t -> dl.map7 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map7 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T2, VD2 : ViewData<T2>, BR2 : BaseRenderer<T2, VD2>,
        T3, VD3 : ViewData<T3>, BR3 : BaseRenderer<T3, VD3>,
        T4, VD4 : ViewData<T4>, BR4 : BaseRenderer<T4, VD4>,
        T5, VD5 : ViewData<T5>, BR5 : BaseRenderer<T5, VD5>,
        T6, VD6 : ViewData<T6>, BR6 : BaseRenderer<T6, VD6>,
        T7, VD7 : ViewData<T7>, BR7 : BaseRenderer<T7, VD7>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T7, BR7>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T7, BR7>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>>.getLast8()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T7, BR7>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T7, BR7>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get8() },
                put = { dl, t -> dl.map8 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map8 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, BR0 : BaseRenderer<T0, VD0>,
        T1, VD1 : ViewData<T1>, BR1 : BaseRenderer<T1, VD1>,
        T2, VD2 : ViewData<T2>, BR2 : BaseRenderer<T2, VD2>,
        T3, VD3 : ViewData<T3>, BR3 : BaseRenderer<T3, VD3>,
        T4, VD4 : ViewData<T4>, BR4 : BaseRenderer<T4, VD4>,
        T5, VD5 : ViewData<T5>, BR5 : BaseRenderer<T5, VD5>,
        T6, VD6 : ViewData<T6>, BR6 : BaseRenderer<T6, VD6>,
        T7, VD7 : ViewData<T7>, BR7 : BaseRenderer<T7, VD7>,
        T8, VD8 : ViewData<T8>, BR8 : BaseRenderer<T8, VD8>,
        T, VD : ViewData<T>, BR : BaseRenderer<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T8, HConsK<ForIdT, T, DL>>>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T7, BR7>, HConsK<ForComposeItem, Pair<T8, BR8>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T7, BR7>, HConsK<ForComposeItemData, Pair<T8, BR8>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>>>.getLast9()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T8, HConsK<ForIdT, T, DL>>>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T7, BR7>, HConsK<ForComposeItem, Pair<T8, BR8>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T7, BR7>, HConsK<ForComposeItemData, Pair<T8, BR8>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>>>.ComposeGetter<T, VD, BR> =
        ComposeGetter(get = { it.get9() },
                put = { dl, t -> dl.map9 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map9 { it.fix().copy(viewData = vd) } })
//</editor-fold>