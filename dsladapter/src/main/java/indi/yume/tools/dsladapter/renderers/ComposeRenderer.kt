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
    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>> add(renderer: BaseRenderer<T, VD, UP>)
            : ComposeBuilder<HConsK<ForIdT, T, DL>,
            HConsK<ForComposeItem, Pair<T, UP>, IL>,
            HConsK<ForComposeItemData, Pair<T, UP>, VDL>> {
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

    inner class ComposeGetter<T, VD : ViewData<T>, UP : Updatable<T, VD>>(
            val get: (IL) -> ComposeOf<T, UP>,
            val put: (DL, T) -> DL,
            val putVD: (VDL, VD) -> VDL) {
        fun up(act: UP.() -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this) { act() }

        fun reduce(act: UP.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> =
                updateItem(this, act)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
            updateItem(getter: ComposeGetter<T, VD, UP>, act: UP.(oldData: T) -> ActionU<VD>): ActionU<ComposeViewData<DL, VDL>> {
        val composeItem = getter.get(renderer.composeList).fix()

        return result@{ oldVD ->
            val (index, target) = oldVD.vdNormalList.asSequence()
                    .withIndex()
                    .find { it.value.item.key == composeItem.key }
            ?: return@result EmptyAction to oldVD

            val targetVD = target.viewData as VD

            val resultAction = composeItem.renderer.updater.act(targetVD.originData)

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


fun <T, VD : ViewData<T>, UP : Updatable<T, VD>>
        itemSub(renderer: BaseRenderer<T, VD, UP>)
        : ComposeItem<T, VD, UP> =
        ComposeItem(renderer)

data class ComposeItem<T, VD : ViewData<T>, UP : Updatable<T, VD>>(
        val renderer: BaseRenderer<T, VD, UP>
) : ComposeOf<T, UP> {
    internal val key = Any()
}

//<editor-fold defaultstate="collapsed" desc="ComposeItem HighType">
class ForComposeItem private constructor() {
    companion object
}
typealias ComposeOf<T, UP> = Kind<ForComposeItem, Pair<T, UP>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Kind<ForComposeItem, Any?>.fixAny(): ComposeItem<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>> =
        this as ComposeItem<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD, UP> ComposeOf<T, UP>.fix(): ComposeItem<T, VD, UP> where VD : ViewData<T>, UP : Updatable<T, VD> =
        this as ComposeItem<T, VD, UP>

//</editor-fold>

data class ComposeItemData<T, VD : ViewData<T>, UP : Updatable<T, VD>>(
        val viewData: VD,
        val item: ComposeItem<T, VD, UP>) : ComposeDataOf<T, UP>

//<editor-fold defaultstate="collapsed" desc="ComposeItemData HighType">
class ForComposeItemData private constructor() {
    companion object
}
typealias ComposeDataOf<T, UP> = Kind<ForComposeItemData, Pair<T, UP>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun Kind<ForComposeItemData, Any?>.fixAny(): ComposeItemData<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>> =
        this as ComposeItemData<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, VD, UP> ComposeDataOf<T, UP>.fix(): ComposeItemData<T, VD, UP> where VD : ViewData<T>, UP : Updatable<T, VD> =
        this as ComposeItemData<T, VD, UP>
//</editor-fold>

data class ComposeViewData<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        override val originData: DL,
        val vdList: VDL) : ViewData<DL> {
    val vdNormalList: List<ComposeItemData<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>> =
            vdList.toList().map { it.fixAny() }

    val endsPoint: IntArray = vdNormalList.getEndsPoints { it.viewData.count }

    override val count: Int = endsPoint.getEndPoint()
}


//<editor-fold defaultstate="collapsed" desc="Updater getter">
/**
 * Mustache:
 *
 * data: {"index": 9, "sum": 10, "list":["0", "1", "2", "3", "4", "5", "6", "7", "8"]}
 *
 * template:
 * fun <{{#list}}T{{.}}, VD{{.}} : ViewData<T{{.}}>, UP{{.}} : Updatable<T{{.}}, VD{{.}}>,
 *         {{/list}}
 *         T, VD : ViewData<T>, UP : Updatable<T, VD>,
 *         DL : HListK<ForIdT, DL>,
 *         IL : HListK<ForComposeItem, IL>,
 *         VDL : HListK<ForComposeItemData, VDL>>
 *         ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, UP{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, UP>, IL>{{#list}}>{{/list}},
 *                 {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, UP{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, UP>, VDL>{{#list}}>{{/list}}>.getLast{{index}}()
 *         : ComposeUpdater<{{#list}}HConsK<ForIdT, T{{.}}, {{/list}}HConsK<ForIdT, T, DL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItem, Pair<T{{.}}, UP{{.}}>, {{/list}}HConsK<ForComposeItem, Pair<T, UP>, IL>{{#list}}>{{/list}},
 *         {{#list}}HConsK<ForComposeItemData, Pair<T{{.}}, UP{{.}}>, {{/list}}HConsK<ForComposeItemData, Pair<T, UP>, VDL>{{#list}}>{{/list}}>.ComposeGetter<T, VD, UP> =
 *         ComposeGetter(get = { it.get{{sum}}() },
 *                 put = { dl, t -> dl.map{{sum}} { IdT(t) } },
 *                 putVD = { vdl, vd -> vdl.map{{sum}} { it.fix().copy(viewData = vd) } })
 */
fun <T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T, DL>,
                HConsK<ForComposeItem, Pair<T, UP>, IL>,
                HConsK<ForComposeItemData, Pair<T, UP>, VDL>>.getLast0()
        : ComposeUpdater<HConsK<ForIdT, T, DL>,
        HConsK<ForComposeItem, Pair<T, UP>, IL>,
        HConsK<ForComposeItemData, Pair<T, UP>, VDL>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get0() },
                put = { dl, t -> dl.map0 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map0 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T, UP>, IL>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>.getLast1()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T, DL>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T, UP>, IL>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get1() },
                put = { dl, t -> dl.map1 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map1 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T, DL>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>.getLast2()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T, DL>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get2() },
                put = { dl, t -> dl.map2 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map2 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>.getLast3()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T, DL>>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get3() },
                put = { dl, t -> dl.map3 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map3 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>.getLast4()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T, DL>>>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get4() },
                put = { dl, t -> dl.map4 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map4 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>.getLast5()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T, DL>>>>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get5() },
                put = { dl, t -> dl.map5 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map5 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>,
        T5, VD5 : ViewData<T5>, UP5 : Updatable<T5, VD5>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>.getLast6()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T, DL>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get6() },
                put = { dl, t -> dl.map6 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map6 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>,
        T5, VD5 : ViewData<T5>, UP5 : Updatable<T5, VD5>,
        T6, VD6 : ViewData<T6>, UP6 : Updatable<T6, VD6>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T6, UP6>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T6, UP6>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>>.getLast7()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T, DL>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T6, UP6>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T6, UP6>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get7() },
                put = { dl, t -> dl.map7 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map7 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>,
        T5, VD5 : ViewData<T5>, UP5 : Updatable<T5, VD5>,
        T6, VD6 : ViewData<T6>, UP6 : Updatable<T6, VD6>,
        T7, VD7 : ViewData<T7>, UP7 : Updatable<T7, VD7>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T6, UP6>, HConsK<ForComposeItem, Pair<T7, UP7>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T6, UP6>, HConsK<ForComposeItemData, Pair<T7, UP7>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>>>.getLast8()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T, DL>>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T6, UP6>, HConsK<ForComposeItem, Pair<T7, UP7>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T6, UP6>, HConsK<ForComposeItemData, Pair<T7, UP7>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get8() },
                put = { dl, t -> dl.map8 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map8 { it.fix().copy(viewData = vd) } })

fun <T0, VD0 : ViewData<T0>, UP0 : Updatable<T0, VD0>,
        T1, VD1 : ViewData<T1>, UP1 : Updatable<T1, VD1>,
        T2, VD2 : ViewData<T2>, UP2 : Updatable<T2, VD2>,
        T3, VD3 : ViewData<T3>, UP3 : Updatable<T3, VD3>,
        T4, VD4 : ViewData<T4>, UP4 : Updatable<T4, VD4>,
        T5, VD5 : ViewData<T5>, UP5 : Updatable<T5, VD5>,
        T6, VD6 : ViewData<T6>, UP6 : Updatable<T6, VD6>,
        T7, VD7 : ViewData<T7>, UP7 : Updatable<T7, VD7>,
        T8, VD8 : ViewData<T8>, UP8 : Updatable<T8, VD8>,
        T, VD : ViewData<T>, UP : Updatable<T, VD>,
        DL : HListK<ForIdT, DL>,
        IL : HListK<ForComposeItem, IL>,
        VDL : HListK<ForComposeItemData, VDL>>
        ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T8, HConsK<ForIdT, T, DL>>>>>>>>>>,
                HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T6, UP6>, HConsK<ForComposeItem, Pair<T7, UP7>, HConsK<ForComposeItem, Pair<T8, UP8>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>>>>,
                HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T6, UP6>, HConsK<ForComposeItemData, Pair<T7, UP7>, HConsK<ForComposeItemData, Pair<T8, UP8>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>>>>.getLast9()
        : ComposeUpdater<HConsK<ForIdT, T0, HConsK<ForIdT, T1, HConsK<ForIdT, T2, HConsK<ForIdT, T3, HConsK<ForIdT, T4, HConsK<ForIdT, T5, HConsK<ForIdT, T6, HConsK<ForIdT, T7, HConsK<ForIdT, T8, HConsK<ForIdT, T, DL>>>>>>>>>>,
        HConsK<ForComposeItem, Pair<T0, UP0>, HConsK<ForComposeItem, Pair<T1, UP1>, HConsK<ForComposeItem, Pair<T2, UP2>, HConsK<ForComposeItem, Pair<T3, UP3>, HConsK<ForComposeItem, Pair<T4, UP4>, HConsK<ForComposeItem, Pair<T5, UP5>, HConsK<ForComposeItem, Pair<T6, UP6>, HConsK<ForComposeItem, Pair<T7, UP7>, HConsK<ForComposeItem, Pair<T8, UP8>, HConsK<ForComposeItem, Pair<T, UP>, IL>>>>>>>>>>,
        HConsK<ForComposeItemData, Pair<T0, UP0>, HConsK<ForComposeItemData, Pair<T1, UP1>, HConsK<ForComposeItemData, Pair<T2, UP2>, HConsK<ForComposeItemData, Pair<T3, UP3>, HConsK<ForComposeItemData, Pair<T4, UP4>, HConsK<ForComposeItemData, Pair<T5, UP5>, HConsK<ForComposeItemData, Pair<T6, UP6>, HConsK<ForComposeItemData, Pair<T7, UP7>, HConsK<ForComposeItemData, Pair<T8, UP8>, HConsK<ForComposeItemData, Pair<T, UP>, VDL>>>>>>>>>>>.ComposeGetter<T, VD, UP> =
        ComposeGetter(get = { it.get9() },
                put = { dl, t -> dl.map9 { IdT(t) } },
                putVD = { vdl, vd -> vdl.map9 { it.fix().copy(viewData = vd) } })
//</editor-fold>