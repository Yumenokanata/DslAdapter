package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.mapT
import indi.yume.tools.dsladapter.type
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updater.SplitUpdater
import indi.yume.tools.dsladapter.updater.Updatable
import java.util.*

/**
 * Sample:
 *
 * SplitRenderer.build<String> {
 *     +LayoutRenderer<String>(layout = R.layout.sample_layout)
 *
 *     !LayoutRenderer<Unit>(layout = R.layout.sample_layout)
 * }
 *
 */
class SplitRenderer<T>(
        val renderers: List<BaseRenderer<T, ViewData<T>>>
): BaseRenderer<T, SplitViewData<T>>() {
    override val defaultUpdater: Updatable<T, SplitViewData<T>> = SplitUpdater(this)

    override fun getData(content: T): SplitViewData<T> {
        val newVDList = renderers.map { it.getData(content) }
        return SplitViewData(content, newVDList)
    }

    override fun getItemId(data: SplitViewData<T>, index: Int): Long {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val vd = data.vdList[resolvedRepositoryIndex]
        val item = renderers[resolvedRepositoryIndex]
        return item.getItemId(vd, resolvedItemIndex)
    }

    override fun getLayoutResId(data: SplitViewData<T>, position: Int): Int {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position, data.endsPoint)
        val vd = data.vdList[resolvedRepositoryIndex]
        val item = renderers[resolvedRepositoryIndex]
        return item.getLayoutResId(vd, resolvedItemIndex)
    }

    override fun bind(data: SplitViewData<T>, index: Int, holder: RecyclerView.ViewHolder) {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        val vd = data.vdList[resolvedRepositoryIndex]
        val item = renderers[resolvedRepositoryIndex]
        item.bind(vd, resolvedItemIndex, holder)
        holder.bindRecycle(this) { item.recycle(it) }
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }

    companion object {
        fun <T> build(f: SplitBuilder<T>.() -> Unit): SplitRenderer<T> {
            val builder = SplitBuilder<T>()
            builder.f()
            return builder.build()
        }
    }
}

data class SplitViewData<T>(
        override val originData: T,
        val vdList: List<ViewData<T>>): ViewData<T> {
    val endsPoint: IntArray = vdList.getEndsPoints { it.count }

    override val count: Int = endsPoint.getEndPoint()
}

class SplitBuilder<T> {
    val renderers: MutableList<BaseRenderer<T, ViewData<T>>> = LinkedList()

    fun <VD: ViewData<T>, BR : BaseRenderer<T, VD>> add(subRenderer: BR) {
        @Suppress("UNCHECKED_CAST")
        renderers += subRenderer as BaseRenderer<T, ViewData<T>>
    }

    fun <VD: ViewData<Unit>, BR : BaseRenderer<Unit, VD>> addUnit(unitRenderer: BR) {
        val r = unitRenderer.mapT(type = type<T>(),
                mapper = { Unit },
                demapper = { oldData, newMapData -> oldData })
        @Suppress("UNCHECKED_CAST")
        renderers.add(r as BaseRenderer<T, ViewData<T>>)
    }

    operator fun <VD: ViewData<T>, BR : BaseRenderer<T, VD>> BR.unaryPlus() = add(this)

    operator fun <VD: ViewData<Unit>, BR : BaseRenderer<Unit, VD>> BR.not() =
            addUnit(this)

    fun build(): SplitRenderer<T> =
            SplitRenderer(renderers.toList())
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> BaseRenderer<T, SplitViewData<T>>.fix(): SplitRenderer<T> =
        this as SplitRenderer<T>