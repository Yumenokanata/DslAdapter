package indi.yume.tools.dsladapter.paging

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.paging.map
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.renderers.EmptyRenderer
import indi.yume.tools.dsladapter.renderers.EmptyViewData
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class ListPagingDataAdapter<T : Any, VD : ViewData<T>, PVD : ViewData<Unit>>(
        val itemRenderer: BaseRenderer<T, VD>,
        val placeholderRenderer: BaseRenderer<Unit, PVD>,
        diffCallback: DiffUtil.ItemCallback<T>)
    : PagingDataAdapter<VD, RecyclerView.ViewHolder>(MockVDDiffCallback(diffCallback)) {

    val placeholderData by lazy { placeholderRenderer.getData(Unit) }

    override fun getItemViewType(position: Int): Int {
        val vd = getItem(position)
        return if (vd == null)
            placeholderRenderer.getItemViewType(placeholderData, 0)
        else
            itemRenderer.getItemViewType(vd, 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder =
            itemRenderer.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vd = getItem(position)
        return if (vd == null)
            placeholderRenderer.bind(placeholderData, 0, holder)
        else
            itemRenderer.bind(vd, 0, holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        itemRenderer.recycle(holder)
        return super.onFailedToRecycleView(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        itemRenderer.recycle(holder)
    }

    suspend fun submitOriData(pagingData: PagingData<T>) {
        submitData(pagingData.map { itemRenderer.getData(it) })
    }

    fun submitOriData(lifecycle: Lifecycle, pagingData: PagingData<T>) {
        submitData(lifecycle, pagingData.map { itemRenderer.getData(it) })
    }

    companion object {

        fun <T : Any, VD : ViewData<T>, PVD : ViewData<Unit>> create(
                itemRenderer: BaseRenderer<T, VD>,
                placeholderRenderer: BaseRenderer<Unit, PVD>,
                diffCallback: DiffUtil.ItemCallback<T>)
                : ListPagingDataAdapter<T, VD, PVD> =
                ListPagingDataAdapter(
                        itemRenderer = itemRenderer,
                        placeholderRenderer = placeholderRenderer,
                        diffCallback = diffCallback
                )

        fun <T : Any, VD : ViewData<T>> createWithoutPlaceholder(
                itemRenderer: BaseRenderer<T, VD>,
                diffCallback: DiffUtil.ItemCallback<T>)
        : ListPagingDataAdapter<T, VD, EmptyViewData<Unit>> =
                ListPagingDataAdapter(
                        itemRenderer = itemRenderer,
                        placeholderRenderer = EmptyRenderer<Unit>(),
                        diffCallback = diffCallback
                )
    }
}

internal class MockVDDiffCallback<T : Any, VD : ViewData<T>>(
        val diffCallback: DiffUtil.ItemCallback<T>)
    : DiffUtil.ItemCallback<VD>() {
    override fun areItemsTheSame(oldItem: VD, newItem: VD): Boolean =
            diffCallback.areItemsTheSame(oldItem.originData, newItem.originData)

    override fun areContentsTheSame(oldItem: VD, newItem: VD): Boolean =
            diffCallback.areContentsTheSame(oldItem.originData, newItem.originData)

}