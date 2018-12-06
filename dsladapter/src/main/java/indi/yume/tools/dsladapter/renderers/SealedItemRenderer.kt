package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.OnInserted
import indi.yume.tools.dsladapter.datatype.OnRemoved
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import kotlin.reflect.KClass

class SealedItemRenderer<T : Any>(
        val sealedList: List<SealedItem<T>>
) : BaseRenderer<T, SealedViewData<T>>() {
    override fun getData(content: T): SealedViewData<T> {
        val item = sealedList.first { it.checker(content) }
        return SealedViewData(item.renderer.getData(item.mapper(content)), item)
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

    override fun getUpdates(oldData: SealedViewData<T>, newData: SealedViewData<T>): List<UpdateActions> {
        return if(oldData.item != newData.item) {
            listOf(
                    OnRemoved(0, oldData.count),
                    OnInserted(0, newData.count)
            )
        } else {
            newData.item.renderer.getUpdates(oldData.data, newData.data)
        }
    }
}

operator fun <T> SealedItem<T>.plus(si2: SealedItem<T>) : List<SealedItem<T>> = listOf(this, si2)

fun <T, V, VD : ViewData> item(checker: (T) -> Boolean,
                               mapper: (T) -> V,
                               renderer: BaseRenderer<V, VD>): SealedItem<T> =
        SealedItem(checker, { mapper(it) as Any }, renderer as BaseRenderer<Any, ViewData>)

fun <T : Any, K : T, V, VD : ViewData> item(clazz: KClass<K>,
                                            mapper: (K) -> V,
                                            renderer: BaseRenderer<V, VD>): SealedItem<T> =
        SealedItem({ clazz.isInstance(it) }, { mapper(it as K) as Any }, renderer as BaseRenderer<Any, ViewData>)

data class SealedItem<T>(
        val checker: (T) -> Boolean,
        val mapper: (T) -> Any,
        val renderer: BaseRenderer<Any, ViewData>
)

data class SealedViewData<T>(val data: ViewData, val item: SealedItem<T>) : ViewData {
    override val count: Int
        get() = data.count
}