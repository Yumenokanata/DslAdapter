package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

/**
 * Created by xuemaotang on 2017/11/16.
 */

class ListRenderer<T, I, IV : ViewData<I>, BR : BaseRenderer<I, IV>>(
        val converter: (T) -> List<I>,
        val demapper: (oldData: T, newMapData: List<I>) -> T,
        val subs: BR,
        val keyGetter: KeyGetter<I>?,
        val itemIsSingle: Boolean = false
) : BaseRenderer<T, ListViewData<T, I, IV>>() {
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

data class ListViewData<T, I, VD : ViewData<I>>(override val originData: T, val data: List<I>, val list: List<VD>) : ViewData<T>, List<VD> by list {
    init {
        assert(list.size == data.size)
    }

    val endsPoint: IntArray = list.getEndsPoints()

    override val count: Int
        get() = endsPoint.getEndPoint()
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T, I, IV : ViewData<I>, BR : BaseRenderer<I, IV>> BaseRenderer<T, ListViewData<T, I, IV>>.fix(): ListRenderer<T, I, IV, BR> =
        this as ListRenderer<T, I, IV, BR>
