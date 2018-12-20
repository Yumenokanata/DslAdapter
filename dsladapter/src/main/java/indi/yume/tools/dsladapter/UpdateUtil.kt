package indi.yume.tools.dsladapter

import arrow.Kind
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.datatype.HListK.Companion.nil
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


//sealed class UpdateDataActions<T>
//
//data class OnInserted<T, I>(val pos: Int, val count: Int, val insertedItems: List<T>) : UpdateDataActions<T>()
//
//data class OnRemoved<T>(val pos: Int, val count: Int, val removedItems: List<T>) : UpdateDataActions<T>()
//
//data class OnMoved<T>(val fromPosition: Int, val toPosition: Int, val item: T) : UpdateDataActions<T>()
//
//data class OnChanged<T>(val pos: Int, val count: Int, val payload: Any?, val oldItems: List<T>, val newItems: List<T>) : UpdateDataActions<T>()
//
//data class ActionComposite<T>(val offset: Int, val actions: List<UpdateDataActions<T>>) : UpdateDataActions<T>()


//typealias Checker<D, VD> = (oldData: D, oldVD: VD, newData: D, newVD: VD) -> List<UpdateDataActions<D>>

//typealias Updater<D, VD> = BaseRenderer<D, VD>.(oldData: D, oldVD: VD, actions: List<UpdateActions>) -> VD

typealias Action<VD> = (VD) -> Pair<UpdateActions, VD>

interface Updatable<P, VD : ViewData<P>> : UpdatableOf<P, VD>

typealias UpdateBuilder<D, VD, UP, U> = BaseRenderer<D, VD, UP>.() -> U

class TypeCheck<T>

private val typeFake = TypeCheck<Nothing>()

@Suppress("UNCHECKED_CAST")
fun <T> type(): TypeCheck<T> = typeFake as TypeCheck<T>

data class IdT<T>(val a: T) : IdTOf<T>

class ForIdT private constructor() { companion object }
typealias IdTOf<T> = Kind<ForIdT, T>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> IdTOf<T>.fix(): IdT<T> =
        this as IdT<T>
@Suppress("NOTHING_TO_INLINE")
inline fun <T> IdTOf<T>.value(): T = fix().a

fun <T> T.toIdT(): IdT<T> = IdT(this)

fun <E> HListK.Companion.singleId(e: E): HConsK<ForIdT, E, HNilK<ForIdT>> = HConsK(IdT(e), nil())

// put item to First
fun <E, L : HListK<ForIdT, L>> L.putF(e: E): HConsK<ForIdT, E, L> = extend(IdT(e))


interface Insertable<P, D : ViewData<P>, I> : Updatable<P, D> {
    fun insert(pos: Int, insertedItems: I): Action<D>
}

interface Removable<P, D: ViewData<P>> : Updatable<P, D> {
    fun remove(pos: Int, count: Int): Action<D>
}

interface Movable<P, D: ViewData<P>> : Updatable<P, D> {
    fun move(fromPosition: Int, toPosition: Int): Action<D>
}

interface Changeable<P, D: ViewData<P>, I> : Updatable<P, D> {
    fun change(pos: Int, payload: Any?, newItems: I): Action<D>
}

interface ListChangeable<P, D: ViewData<P>, I>
    : Insertable<P, D, List<I>>,
        Removable<P, D>,
        Movable<P, D>,
        Changeable<P, D, List<I>>

fun <P, VD : ViewData<P>> updateVD(oldVD: VD, newVD: VD, payload: Any? = null): UpdateActions = when {
        newVD == oldVD -> EmptyAction
        newVD.count == oldVD.count -> OnChanged(0, newVD.count, payload)
        else -> ActionComposite(0, listOf(
                OnRemoved(0, oldVD.count),
                OnInserted(0, newVD.count)
        ))
    }

operator fun <T, VD : ViewData<T>> Action<VD>.plus(a2: Action<VD>): Action<VD> =
        { oldVD ->
            val (firstAct, firstVD) = this@plus(oldVD)
            val (secondAct, secondVD) = a2(firstVD)
            ActionComposite(0, listOf(firstAct, secondAct)) to secondVD
        }


class ForUpdatable private constructor() { companion object }
typealias UpdatableOf<P, VD> = Kind<ForUpdatable, Pair<P, VD>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <P, VD> UpdatableOf<P, VD>.fix(): Updatable<P, VD> where VD : ViewData<P> =
        this as Updatable<P, VD>



//fun <T, I, IV : ViewData, U : Updatable<IV>> listUpdate(subsUpdaterF: BaseRenderer<I, IV>.() -> U)
//        : UpdateBuilder<T, ListViewData<IV>, ListUpdater<T, I, IV, U>> =
//        { listUpdater(subsUpdaterF) }
//
//fun <T, I, IV : ViewData, U : Updatable<IV>> BaseRenderer<T, ListViewData<IV>>
//        .listUpdater(type: TypeCheck<I>, subsUpdaterF: BaseRenderer<I, IV>.() -> U): ListUpdater<T, I, IV, U> =
//        listUpdater(subsUpdaterF)
//
//fun <T, I, IV : ViewData, U : Updatable<IV>> BaseRenderer<T, ListViewData<IV>>
//        .listUpdater(subsUpdaterF: BaseRenderer<I, IV>.() -> U): ListUpdater<T, I, IV, U> {
//    val renderer = this.fix<T, I, IV>()
//    return ListUpdater(renderer, subsUpdaterF(renderer.subs))
//}
//
//fun <T, I, IV : ViewData, U : Updatable<IV>> BaseRenderer<T, ListViewData<IV>>
//        .list(subsUpdater: U): ListUpdater<T, I, IV, U> =
//        ListUpdater(this.fix(), subsUpdater)




//
//fun <T, G, GData: ViewData, I, IData: ViewData, UG : Updatable<GData>, UI : Updatable<IData>> groupItemUpdate(
//        groupUpdater: UG,
//        itemUpdater: UI)
//        : UpdateBuilder<T, GroupViewData<GData, IData>, GroupItemUpdater<T, G, GData, I, IData, UG, UI>> =
//        { groupItemUpdater(groupUpdater, itemUpdater) }
//
//fun <T, G, GData: ViewData, I, IData: ViewData, UG : Updatable<GData>, UI : Updatable<IData>> BaseRenderer<T, GroupViewData<GData, IData>>.groupItemUpdater(
//        groupUpdater: UG,
//        itemUpdater: UI)
//        : GroupItemUpdater<T, G, GData, I, IData, UG, UI> =
//        GroupItemUpdater(this.fix(), groupUpdater, itemUpdater)
//
//class GroupItemUpdater<T, G, GData: ViewData, I, IData: ViewData, UG : Updatable<GData>, UI : Updatable<IData>>(
//        val renderer: TitleItemRenderer<T, G, GData, I, IData>,
//        val groupUpdater: UG,
//        val itemUpdater: UI)
//    : Updatable<GroupViewData<GData, IData>> {
//    fun title(updater: UG.() -> Action<GData>): Action<GroupViewData<GData, IData>> = { oldVD ->
//        val (titleActions, titleVD) = groupUpdater.updater()(oldVD.titleItem)
//
//        ActionComposite(0, listOf(titleActions)) to GroupViewData(titleVD, oldVD.subsData)
//    }
//
//    fun sub(pos: Int, updater: UI.() -> Action<IData>): Action<GroupViewData<GData, IData>> = subs@{ oldVD ->
//        val subsData = oldVD.subsData
//        val targetItem = subsData.getOrNull(pos) ?: return@subs EmptyAction to oldVD
//
//        val (targetActions, targetNewVD) = itemUpdater.updater()(targetItem)
//        val targetRealPos = oldVD.titleSize + oldVD.subEndPoints.getTargetStartPoint(pos)
//        val newItemsVD = oldVD.subsData.toMutableList().apply { set(pos, targetNewVD) }.toList()
//
//        ActionComposite(targetRealPos, listOf(targetActions)) to GroupViewData(oldVD.titleItem, newItemsVD)
//    }
//
//    fun updateTitle(newGroupData: G): Action<GroupViewData<GData, IData>> = { oldVD ->
//        val newGroupVD = renderer.group.getData(newGroupData)
//        updateVD(oldVD.titleItem, newGroupVD) to GroupViewData(newGroupVD, oldVD.subsData)
//    }
//}