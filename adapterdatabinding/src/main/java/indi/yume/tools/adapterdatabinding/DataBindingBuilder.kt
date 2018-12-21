package indi.yume.tools.adapterdatabinding

import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class DataBindingBuilder<I : Any>(val layout: (I) -> Int) {
    private val itemIds: ArrayList<Pair<(I) -> Int, (I) -> Any?>> = ArrayList()
    private val handlers: ArrayList<Pair<Int, Any?>> = ArrayList()
    @RecycleConfig
    private var recycleConfig: Int = DO_NOTHING
    private var stableIdForItem: (I) -> Long = { RecyclerView.NO_ID }
    private var collectionId: Int = DataBindingRenderer.BR_NO_ID

    fun itemId(itemId: Int): DataBindingBuilder<I> {
        itemIds.add({ _: I -> itemId } to { i: I -> i })
        return this
    }

    fun <R> itemId(itemId: Int, mapper: (I) -> R): DataBindingBuilder<I> {
        itemIds.add({ _: I -> itemId } to mapper)
        return this
    }

    fun itemIdForItem(itemIdForItem: (I) -> Int): DataBindingBuilder<I> {
        itemIds.add(itemIdForItem to { i: I -> i })
        return this
    }

    fun <R> itemIdForItem(itemIdForItem: (I) -> Int, mapper: (I) -> R): DataBindingBuilder<I> {
        itemIds.add(itemIdForItem to mapper)
        return this
    }

    fun handler(handlerId: Int, handler: Any): DataBindingBuilder<I> {
        handlers.add(handlerId to handler)
        return this
    }

    fun onRecycle(@RecycleConfig recycleConfig: Int): DataBindingBuilder<I> {
        this.recycleConfig = recycleConfig
        return this
    }

    fun collectionId(collectionId: Int): DataBindingBuilder<I> {
        this.collectionId = collectionId
        return this
    }

    fun stableIdForItem(stableIdForItem: (I) -> Long): DataBindingBuilder<I> {
        this.stableIdForItem = stableIdForItem
        return this
    }

    fun stableId(stableId: Long): DataBindingBuilder<I> {
        stableIdForItem = { stableId }
        return this
    }

    fun forItem(): DataBindingRenderer<I, I> =
            forCollection { Collections.singletonList(it) }

    fun <T : Any> forItem(mapper: (T) -> I): DataBindingRenderer<T, I> =
            forCollection { Collections.singletonList(mapper(it)) }

    fun <TCol> forCollection(converter: (TCol) -> List<I>): DataBindingRenderer<TCol, I> =
            DataBindingRenderer(
                    layout = layout,
                    itemIds = itemIds,
                    handlers = handlers,
                    recycleConfig = recycleConfig,
                    stableIdForItem = stableIdForItem,
                    collectionId = collectionId,
                    converte = converter)

    fun forList(): DataBindingRenderer<List<I>, I> =
            forCollection { it }
}

fun <I : Any> databindingOf(@LayoutRes layout: Int): DataBindingBuilder<I> =
        DataBindingBuilder { layout }

fun <I : Any> databindingOf(layout: (I) -> Int): DataBindingBuilder<I> =
        DataBindingBuilder(layout)