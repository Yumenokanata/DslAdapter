package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import arrow.Kind
import arrow.Kind2
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.typeclass.doNotAffectOriData
import kotlin.reflect.KClass

/**
 *
 * fun test() {
 *     val renderer: SealedItemRenderer<List<String>, HConsK<Kind<ForSealedItem, List<String>>, Pair<Int, LayoutRenderer<Int>>, HConsK<Kind<ForSealedItem, List<String>>, Pair<String, LayoutRenderer<String>>, HNilK<Kind<ForSealedItem, List<String>>>>>> =
 *             SealedItemRenderer(hlistKOf(
 *                     SealedItem<List<String>, String, LayoutViewData<String>, LayoutUpdater<String>, LayoutRenderer<String>>(
 *                             checker = { it.size == 1 },
 *                             mapper = { it.first() },
 *                             renderer = LayoutRenderer(R.layout.abc_action_menu_item_layout)
 *                     ),
 *                     SealedItem<List<String>, Int, LayoutViewData<Int>, LayoutUpdater<Int>, LayoutRenderer<Int>>(
 *                             checker = { it.size == 2 },
 *                             mapper = { it.first().length },
 *                             renderer = LayoutRenderer(R.layout.abc_action_menu_item_layout)
 *                     )
 *             ))
 *
 *     val renderer1 =
 *             SealedItemRenderer(hlistKOf(
 *                     item(type = type<List<String>>(),
 *                             checker = { it.size == 1 },
 *                             mapper = { it.first() },
 *                             renderer = LayoutRenderer<String>(R.layout.abc_action_menu_item_layout)
 *                     ),
 *                     item(
 *                             checker = { it.size == 2 },
 *                             mapper = { it.first().length },
 *                             renderer = LayoutRenderer<Int>(R.layout.abc_action_menu_item_layout)
 *                     ),
 *                     item(
 *                             checker = { it.size == 2 },
 *                             mapper = { it.first().length },
 *                             renderer = LayoutRenderer<Int>(R.layout.abc_action_menu_item_layout))
 *             ))
 *
 *     val u = renderer.updater
 *
 *     u.sealedItem({ tail.head.fix() }) {
 *         update("ss")
 *     }
 * }
 */
class SealedItemRenderer<T, L : HListK<Kind<ForSealedItem, T>, L>>(
        val sealedList: L
) : BaseRenderer<T, SealedViewData<T>, SealedItemUpdater<T, L>>() {
    private val sealedRealList: List<Kind<Kind<ForSealedItem, T>, *>> by lazy { sealedList.toList() }

    override val updater: SealedItemUpdater<T, L> = SealedItemUpdater(this)

    override fun getData(content: T): SealedViewData<T> {
        val item = sealedRealList.find {
            it.fixAny().checker(content)
        }!!.fixAny()
        return SealedViewData<T>(content, item.renderer.getData(item.mapper(content)), item)
    }

    override fun getItemId(data: SealedViewData<T>, index: Int): Long =
            data.item.run { renderer.getItemId(data.data, index) }

    override fun getLayoutResId(data: SealedViewData<T>, index: Int): Int =
            data.item.run { renderer.getLayoutResId(data.data, index) }

    override fun bind(data: SealedViewData<T>, index: Int, holder: RecyclerView.ViewHolder): Unit =
            data.item.run {
                renderer.bind(data.data, index, holder)
                holder.bindRecycle(this) { data.item.renderer.recycle(it) }
            }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }
}


class SealedItemUpdater<T, L : HListK<Kind<ForSealedItem, T>, L>>(
        val renderer: SealedItemRenderer<T, L>)
    : Updatable<T, SealedViewData<T>> {

    fun <D, VD : ViewData<D>, UP : Updatable<D, VD>>
            sealedItem(f: L.() -> SealedItem<T, D, VD, UP>, act: UP.() -> ActionU<VD>): ActionU<SealedViewData<T>> =
            sealedItemReduce(f) { act() }

    @Suppress("UNCHECKED_CAST")
    fun <D, VD : ViewData<D>, UP : Updatable<D, VD>>
            sealedItemReduce(f: L.() -> SealedItem<T, D, VD, UP>, act: UP.(D) -> ActionU<VD>): ActionU<SealedViewData<T>> {
        val sealedItem = renderer.sealedList.f()

        return { oldVD ->
            if (sealedItem == oldVD.item) {
                val targetVD = oldVD.data as VD
                val subAction = sealedItem.renderer.updater.act(targetVD.originData)
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


operator fun <T, D1, VD1 : ViewData<D1>, UP1 : Updatable<D1, VD1>, D2, VD2 : ViewData<D2>, UP2 : Updatable<D2, VD2>>
        SealedItem<T, D1, VD1, UP1>.plus(si2: SealedItem<T, D2, VD2, UP2>)
        : HConsK<Kind<ForSealedItem, T>, Pair<D1, UP1>, HConsK<Kind<ForSealedItem, T>, Pair<D2, UP2>, HNilK<Kind<ForSealedItem, T>>>> =
        HListK.cons(this, HListK.cons(si2, HListK.nil<Kind<ForSealedItem, T>>()))

fun <T, V, VD : ViewData<V>, UP : Updatable<V, VD>>
        item(type: TypeCheck<T>,
             checker: (T) -> Boolean,
             mapper: (T) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BaseRenderer<V, VD, UP>)
        : SealedItem<T, V, VD, UP> =
        item(checker, mapper, demapper, renderer)

fun <T, V, VD : ViewData<V>, UP : Updatable<V, VD>>
        item(checker: (T) -> Boolean,
             mapper: (T) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BaseRenderer<V, VD, UP>)
        : SealedItem<T, V, VD, UP> =
        SealedItem(checker, mapper, demapper, renderer)

fun <T : Any, K : T, V, VD : ViewData<V>, UP : Updatable<V, VD>>
        item(clazz: KClass<K>,
             mapper: (K) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BaseRenderer<V, VD, UP>)
        : SealedItem<T, V, VD, UP> =
        SealedItem({ clazz.isInstance(it) }, { mapper(it as K) }, demapper, renderer)

data class SealedItem<T, D, VD : ViewData<D>, UP : Updatable<D, VD>>(
        val checker: (T) -> Boolean,
        val mapper: (T) -> D,
        val demapper: (oldData: T, newMapData: D) -> T = doNotAffectOriData(),
        val renderer: BaseRenderer<D, VD, UP>
) : SealedItemOf<T, D, UP>

class ForSealedItem private constructor() {
    companion object
}
typealias SealedItemOf<T, D, UP> = Kind2<ForSealedItem, T, Pair<D, UP>>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Kind<Kind<ForSealedItem, T>, Any?>.fixAny(): SealedItem<T, Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>> =
        this as SealedItem<T, Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, D, VD, UP> SealedItemOf<T, D, UP>.fix(): SealedItem<T, D, VD, UP> where VD : ViewData<D>, UP : Updatable<D, VD> =
        this as SealedItem<T, D, VD, UP>

data class SealedViewData<T>(override val originData: T,
                             val data: ViewData<Any?>,
                             val item: SealedItem<T, Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>) : ViewData<T> {
    override val count: Int
        get() = data.count
}
