package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import arrow.Kind
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ComposeRenderer<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val composeList: IL,
        val getMapper: (DL, IL) -> VDL
) : BaseRenderer<DL, ComposeViewData<DL, VDL>, ComposeUpdater<DL, IL, VDL>>() {
    override val updater: ComposeUpdater<DL, IL, VDL> = ComposeUpdater(this)

    override fun getData(content: DL): ComposeViewData<DL, VDL> {
        return ComposeViewData(content, getMapper(content, composeList))
    }

    override fun getItemId(data: ComposeViewData<DL, VDL>, index: Int): Long {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.vdNormalList[resolvedRepositoryIndex]
        return item.renderer.getItemId(vd, resolvedItemIndex)
    }

    override fun getLayoutResId(data: ComposeViewData<DL, VDL>, index: Int): Int {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.vdNormalList[resolvedRepositoryIndex]
        return item.renderer.getLayoutResId(vd, resolvedItemIndex)
    }

    override fun bind(data: ComposeViewData<DL, VDL>, index: Int, holder: RecyclerView.ViewHolder) {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.vdNormalList[resolvedRepositoryIndex]
        item.renderer.bind(vd, resolvedItemIndex, holder)
        holder.bindRecycle(this) { item.renderer.recycle(it) }
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }

    companion object {
        val startBuild: ComposeBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> =
                ComposeBuilder(HListK.nil()) { _, _ -> HListK.nil() }
    }
}

class ComposeBuilder<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val composeList: IL,
        val getMapper: (DL, IL) -> VDL
) {
    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>> add(renderer: BR)
            : ComposeBuilder<HConsK<ForIdT, T, DL>,
            HConsK<ForComposeItem, Pair<T, BR>, IL>,
            HConsK<ForComposeItemData, Pair<T, BR>, VDL>> {
        return ComposeBuilder(composeList.extend(ComposeItem(renderer)))
        { dl, il ->
            val item = il.head.fix()
            getMapper(dl.tail, il.tail)
                    .extend(ComposeItemData(item.renderer.getData(dl.head.value()), item))
        }
    }

    val start : ComposeBuilder<DL, IL, VDL> = this

    fun build(): ComposeRenderer<DL, IL, VDL> = ComposeRenderer(composeList, getMapper)
}


class ComposeUpdater<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val renderer: ComposeRenderer<DL, IL, VDL>)
    : Updatable<DL, ComposeViewData<DL, VDL>> {

    inner class ComposeGetter<T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>(
            val get: (IL) -> ComposeOf<T, BR>,
            val put: (DL, T) -> DL,
            val putVD: (VDL, VD) -> VDL) {
        fun up(act: UP.() -> Action<VD>): Action<ComposeViewData<DL, VDL>> =
                updateItem(this, act)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>
            updateItem(getter: ComposeGetter<T, VD, UP, BR>, act: UP.() -> Action<VD>): Action<ComposeViewData<DL, VDL>> {
        val composeItem = getter.get(renderer.composeList).fix()

        return result@{ oldVD ->
            val (index, target) = oldVD.vdNormalList.asSequence()
                    .withIndex()
                    .find { it.value.item.key == composeItem.key }
            ?: return@result EmptyAction to oldVD

            val resultAction = composeItem.renderer.updater.act()

            val (actions, subVD) = resultAction(target.viewData as VD)
            val newDL = getter.put(oldVD.originData, subVD.originData)
            val newVDL = getter.putVD(oldVD.vdList, subVD)

            val realTargetStartIndex = oldVD.vdNormalList.take(index).sumBy { it.viewData.count }

            ActionComposite(realTargetStartIndex, listOf(actions)) to ComposeViewData(newDL, newVDL)
        }
    }

    fun updateBy(act: ComposeUpdater<DL, IL, VDL>.() -> Action<ComposeViewData<DL, VDL>>): Action<ComposeViewData<DL, VDL>> =
            act()

    fun update(data: DL, payload: Any? = null): Action<ComposeViewData<DL, VDL>> = { oldVD ->
        val newVD = renderer.getData(data)
        updateVD(oldVD, newVD, payload) to newVD
    }
}


fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>
        itemSub(renderer: BR)
        : ComposeItem<T, VD, UP, BR> =
        ComposeItem(renderer)

data class ComposeItem<T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>(
        val renderer: BR
) : ComposeOf<T, BR> {
    internal val key = Any()
}

//<editor-fold defaultstate="collapsed" desc="ComposeItem HighType">
class ForComposeItem private constructor() {
    companion object
}
typealias ComposeOf<T, BR> = Kind<ForComposeItem, Pair<T, BR>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Kind<ForComposeItem, Any?>.fixAny(): ComposeItem<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>, BaseRenderer<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>> =
        this as ComposeItem<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>, BaseRenderer<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD, UP, BR> ComposeOf<T, BR>.fix(): ComposeItem<T, VD, UP, BR> where VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP> =
        this as ComposeItem<T, VD, UP, BR>

//</editor-fold>

data class ComposeItemData<T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>>(
        val viewData: VD,
        val item: ComposeItem<T, VD, UP, BR>) : ComposeDataOf<T, BR>

//<editor-fold defaultstate="collapsed" desc="ComposeItemData HighType">
class ForComposeItemData private constructor() {
    companion object
}
typealias ComposeDataOf<T, BR> = Kind<ForComposeItemData, Pair<T, BR>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun Kind<ForComposeItemData, Any?>.fixAny(): ComposeItemData<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>, BaseRenderer<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>> =
        this as ComposeItemData<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>, BaseRenderer<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD, UP, BR> ComposeDataOf<T, BR>.fix(): ComposeItemData<T, VD, UP, BR> where VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP> =
        this as ComposeItemData<T, VD, UP, BR>
//</editor-fold>

data class ComposeViewData<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        override val originData: DL,
        val vdList: VDL) : ViewData<DL> {
    val vdNormalList: List<ComposeItemData<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>, BaseRenderer<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>>> =
            vdList.toList().map { it.fixAny() }

    val endsPoint: IntArray = vdNormalList.getEndsPonints { it.viewData.count }

    override val count: Int = endsPoint.getEndPoint()
}


//<editor-fold defaultstate="collapsed" desc="Updater getter">
/**
 * Mustache:
 *
 * data: {"sum": 5, "list":["0", "1", "2", "3"]}
 *
 * template:
 * fun <{{#list}}T{{.}}, VD{{.}} : ViewData<T{{.}}>, UP{{.}} : Updatable<T{{.}}, VD{{.}}>, BR{{.}} : BaseRenderer<T{{.}}, VD{{.}}, UP{{.}}>,
 *         {{/list}}
 *         T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
 *         DL : HListK<ForIdT, DL>,
 *         IL : HListK<ForComposeItem, IL>,
 *         VDL : HListK<ForComposeItemData, VDL>>
 *         ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, BR>, IL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, BR>, VDL>{{#list}}>{{/list}}>.getLast{{sum}}()
 *         : ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, BR>, IL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, BR{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, BR>, VDL>{{#list}}>{{/list}}>.ComposeGetter<T, VD, UP, BR> =
 *         ComposeGetter(get = { it.get{{sum}}() },
 *                 put = { dl, t -> dl.map{{sum}} { IdT(t) } },
 *                 putVD = { vdl, vd -> vdl.map{{sum}} { it.fix().copy(viewData = vd) } })
 */
fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T, DL>,
                HConsK<ForComposeItem, Pair<T, BR>, IL>,
                HConsK<ForComposeItemData, Pair<T, BR>, VDL>>.getLast1()
        : ComposeUpdater<HConsK<ForIdT, T, DL>,
        HConsK<ForComposeItem, Pair<T, BR>, IL>,
        HConsK<ForComposeItemData, Pair<T, BR>, VDL>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get1() },
                put = { dl, t -> dl.map1 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map1 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T, BR>, IL>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>.getLast2()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T, BR>, IL>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get2() },
                put = { dl, t -> dl.map2 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map2 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>, BR1 : BaseRenderer<T1, VD1, UP1>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T1, HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>>,
                HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>,
                HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>.getLast3()
        : ComposeUpdater<HConsK<ForIdT, T1, HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>>,
        HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>,
        HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get3() },
                put = { dl, t -> dl.map3 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map3 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>, BR1 : BaseRenderer<T1, VD1, UP1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>, BR2 : BaseRenderer<T2, VD2, UP2>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>.getLast4()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get4() },
                put = { dl, t -> dl.map4 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map4 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>, BR1 : BaseRenderer<T1, VD1, UP1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>, BR2 : BaseRenderer<T2, VD2, UP2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>, BR3 : BaseRenderer<T3, VD3, UP3>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>.getLast5()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get5() },
                put = { dl, t -> dl.map5 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map5 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>, BR1 : BaseRenderer<T1, VD1, UP1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>, BR2 : BaseRenderer<T2, VD2, UP2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>, BR3 : BaseRenderer<T3, VD3, UP3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>, BR4 : BaseRenderer<T4, VD4, UP4>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>.getLast6()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get6() },
                put = { dl, t -> dl.map6 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map6 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>, BR1 : BaseRenderer<T1, VD1, UP1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>, BR2 : BaseRenderer<T2, VD2, UP2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>, BR3 : BaseRenderer<T3, VD3, UP3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>, BR4 : BaseRenderer<T4, VD4, UP4>,
        T5, VD5 : ViewData<T5>, UP5 : Updatable<T5, VD5>, BR5 : BaseRenderer<T5, VD5, UP5>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>.getLast7()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get7() },
                put = { dl, t -> dl.map7 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map7 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>, BR1 : BaseRenderer<T1, VD1, UP1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>, BR2 : BaseRenderer<T2, VD2, UP2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>, BR3 : BaseRenderer<T3, VD3, UP3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>, BR4 : BaseRenderer<T4, VD4, UP4>,
        T5, VD5 : ViewData<T5>, UP5 : Updatable<T5, VD5>, BR5 : BaseRenderer<T5, VD5, UP5>,
        T6, VD6 : ViewData<T6>, UP6 : Updatable<T6, VD6>, BR6 : BaseRenderer<T6, VD6, UP6>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>.getLast8()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get8() },
                put = { dl, t -> dl.map8 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map8 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>, BR0 : BaseRenderer<T0, VD0, UP0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>, BR1 : BaseRenderer<T1, VD1, UP1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>, BR2 : BaseRenderer<T2, VD2, UP2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>, BR3 : BaseRenderer<T3, VD3, UP3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>, BR4 : BaseRenderer<T4, VD4, UP4>,
        T5, VD5 : ViewData<T5>, UP5 : Updatable<T5, VD5>, BR5 : BaseRenderer<T5, VD5, UP5>,
        T6, VD6 : ViewData<T6>, UP6 : Updatable<T6, VD6>, BR6 : BaseRenderer<T6, VD6, UP6>,
        T7, VD7 : ViewData<T7>, UP7 : Updatable<T7, VD7>, BR7 : BaseRenderer<T7, VD7, UP7>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>, BR : BaseRenderer<T, VD, UP>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T7, BR7>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T7, BR7>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>>.getLast9()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, BR0>, HConsK<ForComposeItem, Pair<T1, BR1>, HConsK<ForComposeItem, Pair<T2, BR2>, HConsK<ForComposeItem, Pair<T3, BR3>, HConsK<ForComposeItem, Pair<T4, BR4>, HConsK<ForComposeItem, Pair<T5, BR5>, HConsK<ForComposeItem, Pair<T6, BR6>, HConsK<ForComposeItem, Pair<T7, BR7>, HConsK<ForComposeItem, Pair<T, BR>, IL>>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, BR0>, HConsK<ForComposeItemData, Pair<T1, BR1>, HConsK<ForComposeItemData, Pair<T2, BR2>, HConsK<ForComposeItemData, Pair<T3, BR3>, HConsK<ForComposeItemData, Pair<T4, BR4>, HConsK<ForComposeItemData, Pair<T5, BR5>, HConsK<ForComposeItemData, Pair<T6, BR6>, HConsK<ForComposeItemData, Pair<T7, BR7>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>>>>>>>>>>.ComposeGetter<T, VD, UP, BR> =
        ComposeGetter(get = { it.get9() },
                put = { dl, t -> dl.map9 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map9 { it.fix().copy(viewData = vd) } })
//</editor-fold>