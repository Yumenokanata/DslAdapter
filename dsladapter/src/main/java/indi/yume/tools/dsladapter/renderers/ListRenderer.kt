package indi.yume.tools.dsladapter.renderers

import android.support.v7.widget.RecyclerView
import indi.yume.tools.dsladapter.Action
import indi.yume.tools.dsladapter.ListChangeable
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.typeclass.doNotAffectOriData
import indi.yume.tools.dsladapter.updateVD

/**
 * Created by xuemaotang on 2017/11/16.
 */

class ListRenderer<T, I, IV : ViewData<I>, UP : Updatable<I, IV>>(
        val converter: (T) -> List<I>,
        val demapper: (oldData: T, newMapData: List<I>) -> T,
        val subs: BaseRenderer<I, IV, UP>
) : BaseRenderer<T, ListViewData<T, I, IV>, ListUpdater<T, I, IV, UP>>() {
    override val updater: ListUpdater<T, I, IV, UP> = ListUpdater(this, subs.updater)

    override fun getData(content: T): ListViewData<T, I, IV> {
        val oriData = converter(content)
        return ListViewData(content, oriData, oriData.map { subs.getData(it) })
    }

    override fun getItemId(data: ListViewData<T, I, IV>, index: Int): Long {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        return subs.getItemId(data[resolvedRepositoryIndex], resolvedItemIndex)
    }

    override fun getLayoutResId(data: ListViewData<T, I, IV>, index: Int): Int {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        return subs.getLayoutResId(data[resolvedRepositoryIndex], resolvedItemIndex)
    }

    override fun bind(data: ListViewData<T, I, IV>, index: Int, holder: RecyclerView.ViewHolder) {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(index, data.endsPoint)
        subs.bind(data[resolvedRepositoryIndex], resolvedItemIndex, holder)
    }

    override fun recycle(holder: RecyclerView.ViewHolder) {
        subs.recycle(holder)
    }
}

class ListViewData<T, I, VD : ViewData<I>>(override val originData: T, val data: List<I>, val list: List<VD>) : ViewData<T>, List<VD> by list {
    init {
        assert(list.size == data.size)
    }

    val endsPoint: IntArray = list.getEndsPonints()

    override val count: Int
        get() = endsPoint.getEndPoint()
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, I, IV : ViewData<I>, UP : Updatable<I, IV>> BaseRenderer<T, ListViewData<T, I, IV>, ListUpdater<T, I, IV, UP>>.fix(): ListRenderer<T, I, IV, UP> =
        this as ListRenderer<T, I, IV, UP>

class ListUpdater<T, I, IV : ViewData<I>, U : Updatable<I, IV>>(
        val renderer: ListRenderer<T, I, IV, U>,
        val subsUpdater: U) : ListChangeable<T, ListViewData<T, I, IV>, I> {
    fun update(data: T, payload: Any? = null): Action<ListViewData<T, I, IV>> = { oldVD ->
        val newVD = renderer.getData(data)
        updateVD(oldVD, newVD, payload) to newVD
    }

    fun subs(pos: Int, updater: U.() -> Action<IV>): Action<ListViewData<T, I, IV>> {
        val subAction = subsUpdater.updater()

        return subsAct@{ oldVD ->
            val subVD = oldVD.list.getOrNull(pos) ?: return@subsAct EmptyAction to oldVD

            val (subActions, subNewVD) = subAction(subVD)
            val newData = oldVD.data.toMutableList().apply { set(pos, subNewVD.originData) }.toList()
            val newVD = oldVD.list.toMutableList().apply { set(pos, subNewVD) }.toList()

            val newOriData = renderer.demapper(oldVD.originData, newData)

            ActionComposite(pos, listOf(subActions)) to ListViewData(newOriData, newData, newVD)
        }
    }

    override fun insert(pos: Int, insertedItems: List<I>): Action<ListViewData<T, I, IV>> =
            { oldVD ->
                val newData = oldVD.data.toMutableList().apply {
                    addAll(pos, insertedItems)
                }.toList()
                val newVD = oldVD.list.toMutableList().apply {
                    addAll(pos, insertedItems.map { renderer.subs.getData(it) })
                }.toList()
                val newOriData = renderer.demapper(oldVD.originData, newData)

                OnInserted(pos, insertedItems.size) to ListViewData(newOriData, newData, newVD)
            }

    override fun remove(pos: Int, count: Int): Action<ListViewData<T, I, IV>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun move(fromPosition: Int, toPosition: Int): Action<ListViewData<T, I, IV>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun change(pos: Int, payload: Any?, newItems: List<I>): Action<ListViewData<T, I, IV>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}