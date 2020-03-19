package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.typeclass.awaysReturnNewData
import indi.yume.tools.dsladapter.typeclass.doNotAffectOriData
import indi.yume.tools.dsladapter.updater.CaseUpdater
import indi.yume.tools.dsladapter.updater.Updatable
import java.util.*

/**
 * Sample:
 *
 * CaseRenderer.build<String> {
 *     caseItem(CaseItem(
 *             checker = { true },
 *             mapper = { it },
 *             demapper = awaysReturnNewData(),
 *             renderer = LayoutRenderer(layout = 1)
 *     ))
 *
 *     caseItemSame(CaseItem(
 *             checker = { true },
 *             mapper = { it },
 *             demapper = awaysReturnNewData(),
 *             renderer = LayoutRenderer(layout = 1)
 *     ))
 *
 *     case(checker = { true },
 *             mapper = { Unit },
 *             renderer = LayoutRenderer<Unit>(layout = 1))
 *
 *     case(checker = { true },
 *             demapper = doNotAffectOriData(),
 *             renderer = LayoutRenderer(layout = 1))
 *
 *     elseCase(mapper = { Unit },
 *             renderer = LayoutRenderer<Unit>(layout = 1))
 *
 *     elseCase(renderer = LayoutRenderer(layout = 1))
 * }
 *
 */
class CaseRenderer<T>(
        val caseList: List<CaseItem<T, Any?, ViewData<Any?>>>
) : BaseRenderer<T, CaseViewData<T>>() {
    override val defaultUpdater: Updatable<T, CaseViewData<T>> = CaseUpdater(this)

    override fun getData(content: T): CaseViewData<T> {
        val item = caseList.find {
            it.checker(content)
        }!!
        return CaseViewData<T>(content, item.renderer.getData(item.mapper(content)), item)
    }

    override fun getItemId(data: CaseViewData<T>, index: Int): Long =
            data.item.run { renderer.getItemId(data.vd, index) }

    override fun getLayoutResId(data: CaseViewData<T>, index: Int): Int =
            data.item.run { renderer.getLayoutResId(data.vd, index) }

    override fun bind(data: CaseViewData<T>, index: Int, holder: RecyclerView.ViewHolder): Unit =
            data.item.run {
                renderer.bind(data.vd, index, holder)
                holder.bindRecycle(this) { data.item.renderer.recycle(it) }
            }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        holder.doRecycle(this)
    }

    companion object {
        fun <T> build(f: CaseBuilder<T>.() -> Unit): CaseRenderer<T> {
            val builder = CaseBuilder<T>()
            builder.f()
            return builder.build()
        }
    }
}

data class CaseItem<T, D, VD : ViewData<D>>(
        val checker: (T) -> Boolean,
        val mapper: (T) -> D,
        val demapper: (oldData: T, newMapData: D) -> T = doNotAffectOriData(),
        val renderer: BaseRenderer<D, VD>
)

data class CaseViewData<T>(
        override val originData: T,
        val vd: ViewData<Any?>,
        val item: CaseItem<T, Any?, ViewData<Any?>>
) : ViewData<T> {
    override val count: Int = vd.count
}

class CaseBuilder<T> {
    val caseList: MutableList<CaseItem<T, Any?, ViewData<Any?>>> = LinkedList()
    var elseCaseItem: CaseItem<T, Any?, ViewData<Any?>>? = null

    fun caseItem(item: CaseItem<T, *, *>) {
        @Suppress("UNCHECKED_CAST")
        caseList += item as CaseItem<T, Any?, ViewData<Any?>>
    }

    fun caseItemSame(item: CaseItem<T, T, *>) {
        @Suppress("UNCHECKED_CAST")
        caseList += item as CaseItem<T, Any?, ViewData<Any?>>
    }

    fun <D, VD : ViewData<D>> case(
            checker: (T) -> Boolean,
            mapper: (T) -> D,
            demapper: (oldData: T, newMapData: D) -> T = doNotAffectOriData(),
            renderer: BaseRenderer<D, VD>) {
        caseItem(CaseItem(checker, mapper, demapper, renderer))
    }

    fun <VD : ViewData<T>> case(
            checker: (T) -> Boolean,
            demapper: (oldData: T, newMapData: T) -> T = awaysReturnNewData(),
            renderer: BaseRenderer<T, VD>) {
        caseItem(CaseItem(checker, { it }, demapper, renderer))
    }

    fun <D, VD : ViewData<D>> elseCase(
            mapper: (T) -> D,
            demapper: (oldData: T, newMapData: D) -> T = doNotAffectOriData(),
            renderer: BaseRenderer<D, VD>) {
        @Suppress("UNCHECKED_CAST")
        elseCaseItem = CaseItem({ true }, mapper, demapper, renderer) as CaseItem<T, Any?, ViewData<Any?>>
    }

    fun <VD : ViewData<T>> elseCase(
            demapper: (oldData: T, newMapData: T) -> T = awaysReturnNewData(),
            renderer: BaseRenderer<T, VD>) {
        elseCase({ it }, demapper, renderer)
    }

    fun build(): CaseRenderer<T> = CaseRenderer(caseList.apply {
        @Suppress("UNCHECKED_CAST")
        val item = elseCaseItem
                ?: CaseItem({ true }, { it }, doNotAffectOriData<T ,T>(),
                        EmptyRenderer<T>()) as CaseItem<T, Any?, ViewData<Any?>>
        this += item
    }.toList())
}

fun test() {
    CaseRenderer.build<String> {
        caseItem(CaseItem(
                checker = { true },
                mapper = { it },
                demapper = awaysReturnNewData(),
                renderer = LayoutRenderer(layout = 1)
        ))

        caseItemSame(CaseItem(
                checker = { true },
                mapper = { it },
                demapper = awaysReturnNewData(),
                renderer = LayoutRenderer(layout = 1)
        ))

        case(checker = { true },
                mapper = { Unit },
                renderer = LayoutRenderer<Unit>(layout = 1))

        case(checker = { true },
                demapper = doNotAffectOriData(),
                renderer = LayoutRenderer(layout = 1))

        elseCase(mapper = { Unit },
                renderer = LayoutRenderer<Unit>(layout = 1))

        elseCase(renderer = LayoutRenderer(layout = 1))
    }
}

fun <T> BaseRenderer<T, CaseViewData<T>>.fix(): CaseRenderer<T> =
        this as CaseRenderer<T>