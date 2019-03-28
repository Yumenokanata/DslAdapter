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
) : BaseRenderer<T, SealedViewData<T, L>>() {
    private val sealedRealList: List<Kind<Kind<ForSealedItem, T>, *>> by lazy { sealedList.toList() }

    override fun getData(content: T): SealedViewData<T, L> {
        val item = sealedRealList.find {
            it.fixAny().checker(content)
        }!!.fixAny()
        return SealedViewData<T, L>(content, item.renderer.getData(item.mapper(content)), item)
    }

    override fun getItemId(data: SealedViewData<T, L>, index: Int): Long =
            data.item.run { renderer.getItemId(data.data, index) }

    override fun getLayoutResId(data: SealedViewData<T, L>, index: Int): Int =
            data.item.run { renderer.getLayoutResId(data.data, index) }

    override fun bind(data: SealedViewData<T, L>, index: Int, holder: RecyclerView.ViewHolder): Unit =
            data.item.run {
                renderer.bind(data.data, index, holder)
                holder.bindRecycle(this) { data.item.renderer.recycle(it) }
            }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }
}



operator fun <T, D1, VD1 : ViewData<D1>, D2, VD2 : ViewData<D2>>
        SealedItem<T, D1, VD1>.plus(si2: SealedItem<T, D2, VD2>)
        : HConsK<Kind<ForSealedItem, T>, Pair<D1, VD1>, HConsK<Kind<ForSealedItem, T>, Pair<D2, VD2>, HNilK<Kind<ForSealedItem, T>>>> =
        HListK.cons(this, HListK.cons(si2, HListK.nil<Kind<ForSealedItem, T>>()))

fun <T, V, VD : ViewData<V>>
        item(type: TypeCheck<T>,
             checker: (T) -> Boolean,
             mapper: (T) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BaseRenderer<V, VD>)
        : SealedItem<T, V, VD> =
        item(checker, mapper, demapper, renderer)

fun <T, V, VD : ViewData<V>>
        item(checker: (T) -> Boolean,
             mapper: (T) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BaseRenderer<V, VD>)
        : SealedItem<T, V, VD> =
        SealedItem(checker, mapper, demapper, renderer)

fun <T : Any, K : T, V, VD : ViewData<V>>
        item(clazz: KClass<K>,
             mapper: (K) -> V,
             demapper: (oldData: T, newMapData: V) -> T = doNotAffectOriData(),
             renderer: BaseRenderer<V, VD>)
        : SealedItem<T, V, VD> =
        SealedItem({ clazz.java.isInstance(it) }, { mapper(it as K) }, demapper, renderer)

data class SealedItem<T, D, VD : ViewData<D>>(
        val checker: (T) -> Boolean,
        val mapper: (T) -> D,
        val demapper: (oldData: T, newMapData: D) -> T = doNotAffectOriData(),
        val renderer: BaseRenderer<D, VD>
) : SealedItemOf<T, D, VD>

class ForSealedItem private constructor() {
    companion object
}
typealias SealedItemOf<T, D, VD> = Kind2<ForSealedItem, T, Pair<D, VD>>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Kind<Kind<ForSealedItem, T>, Any?>.fixAny(): SealedItem<T, Any?, ViewData<Any?>> =
        this as SealedItem<T, Any?, ViewData<Any?>>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, D, VD> SealedItemOf<T, D, VD>.fix(): SealedItem<T, D, VD> where VD : ViewData<D> =
        this as SealedItem<T, D, VD>

data class SealedViewData<T, L : HListK<Kind<ForSealedItem, T>, L>>(
        override val originData: T,
        val data: ViewData<Any?>,
        val item: SealedItem<T, Any?, ViewData<Any?>>) : ViewData<T> {
    override val count: Int
        get() = data.count
}

fun <T, L : HListK<Kind<ForSealedItem, T>, L>> BaseRenderer<T, SealedViewData<T, L>>.fix(): SealedItemRenderer<T, L> =
        this as SealedItemRenderer<T, L>
