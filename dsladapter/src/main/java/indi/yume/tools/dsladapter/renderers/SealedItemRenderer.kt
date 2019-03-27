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
) : BaseRenderer<T, SealedViewData<T>>() {
    private val sealedRealList: List<Kind<Kind<ForSealedItem, T>, *>> by lazy { sealedList.toList() }

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



operator fun <T, D1, VD1 : ViewData<D1>, BR1 : BaseRenderer<D1, VD1>, D2, VD2 : ViewData<D2>, BR2 : BaseRenderer<D2, VD2>>
        SealedItem<T, D1, VD1, BR1>.plus(si2: SealedItem<T, D2, VD2, BR2>)
        : HConsK<Kind<ForSealedItem, T>, Pair<D1, BR1>, HConsK<Kind<ForSealedItem, T>, Pair<D2, BR2>, HNilK<Kind<ForSealedItem, T>>>> =
        HListK.cons(this, HListK.cons(si2, HListK.nil<Kind<ForSealedItem, T>>()))

fun <T, V, VD : ViewData<V>, BR : BaseRenderer<V, VD>>
        item(type: TypeCheck<T>,
             checker: (T) -> Boolean,
             mapper: (T) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BR)
        : SealedItem<T, V, VD, BR> =
        item(checker, mapper, demapper, renderer)

fun <T, V, VD : ViewData<V>, BR : BaseRenderer<V, VD>>
        item(checker: (T) -> Boolean,
             mapper: (T) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BR)
        : SealedItem<T, V, VD, BR> =
        SealedItem(checker, mapper, demapper, renderer)

fun <T : Any, K : T, V, VD : ViewData<V>, BR : BaseRenderer<V, VD>>
        item(clazz: KClass<K>,
             mapper: (K) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BR)
        : SealedItem<T, V, VD, BR> =
        SealedItem({ clazz.isInstance(it) }, { mapper(it as K) }, demapper, renderer)

data class SealedItem<T, D, VD : ViewData<D>, BR : BaseRenderer<D, VD>>(
        val checker: (T) -> Boolean,
        val mapper: (T) -> D,
        val demapper: (oldData: T, newMapData: D) -> T = doNotAffectOriData(),
        val renderer: BR
) : SealedItemOf<T, D, BR>

class ForSealedItem private constructor() {
    companion object
}
typealias SealedItemOf<T, D, BR> = Kind2<ForSealedItem, T, Pair<D, BR>>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Kind<Kind<ForSealedItem, T>, Any?>.fixAny(): SealedItem<T, Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>> =
        this as SealedItem<T, Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, D, VD, BR> SealedItemOf<T, D, BR>.fix(): SealedItem<T, D, VD, BR> where VD : ViewData<D>, BR : BaseRenderer<D, VD> =
        this as SealedItem<T, D, VD, BR>

data class SealedViewData<T>(override val originData: T,
                             val data: ViewData<Any?>,
                             val item: SealedItem<T, Any?, ViewData<Any?>, BaseRenderer<Any?, ViewData<Any?>>>) : ViewData<T> {
    override val count: Int
        get() = data.count
}
