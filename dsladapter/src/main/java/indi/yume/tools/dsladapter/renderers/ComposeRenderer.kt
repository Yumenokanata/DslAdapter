package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.ActionComposite
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ComposeRenderer<T>(
        val composeList: List<SubItem<T>>
) : BaseRenderer<T, ComposeViewData<T>>() {
    override fun getData(content: T): ComposeViewData<T> {
        val data = composeList.map { it.renderer.getData(it.mapper(content)) to it }
        return ComposeViewData(data)
    }

    override fun getItemId(data: ComposeViewData<T>, index: Int): Long {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.list[resolvedRepositoryIndex]
        return item.renderer.getItemId(vd, resolvedItemIndex)
    }

    override fun getLayoutResId(data: ComposeViewData<T>, index: Int): Int {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.list[resolvedRepositoryIndex]
        return item.renderer.getLayoutResId(vd, resolvedItemIndex)
    }

    override fun bind(data: ComposeViewData<T>, index: Int, holder: RecyclerView.ViewHolder) {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val (vd, item) = data.list[resolvedRepositoryIndex]
        item.renderer.bind(vd, resolvedItemIndex, holder)
        holder.bindRecycle(this) { item.renderer.recycle(it) }
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }

    override fun getUpdates(oldData: ComposeViewData<T>, newData: ComposeViewData<T>): List<UpdateActions> {
        return oldData.list.zip(newData.list)
                .mapIndexed { index, (oldItem, newItem) ->
                    val subActions = newItem.second.renderer.getUpdates(oldItem.first, newItem.first)
                    if (subActions.isEmpty())
                        null
                    else
                    ActionComposite(actions = subActions,
                            offset = if (index <= 0) 0 else oldData.endsPoint[index - 1])
                }
                .filterNotNull()
    }

    companion object {
        fun <T> startBuild(f: ComposeBuilder<T>.() -> Unit): ComposeRenderer<T> {
            val builder = ComposeBuilder<T>()
            builder.f()
            return builder.build()
        }
    }
}

class ComposeBuilder<T> {
    val list: MutableList<SubItem<T>> = ArrayList()

    fun <V, VD : ViewData> plus(mapper: (T) -> V,
                                renderer: BaseRenderer<V, VD>): ComposeBuilder<T> {
        list.add(SubItem({ mapper(it) as Any }, renderer as BaseRenderer<Any, ViewData>))
        return this
    }

    fun <VD : ViewData> plus(renderer: BaseRenderer<T, VD>): ComposeBuilder<T> {
        list.add(SubItem({ it as Any }, renderer as BaseRenderer<Any, ViewData>))
        return this
    }

    fun build(): ComposeRenderer<T> = ComposeRenderer(list.toList())
}


data class SubItem<T>(
        val mapper: (T) -> Any,
        val renderer: BaseRenderer<Any, ViewData>
)

data class ComposeViewData<T>(val list: List<Pair<ViewData, SubItem<T>>>) : ViewData {
    val endsPoint: IntArray = list.getEndsPonints { it.first.count }

    override val count: Int = endsPoint.getEndPoint()
}