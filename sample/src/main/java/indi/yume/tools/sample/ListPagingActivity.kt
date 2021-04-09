package indi.yume.tools.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.paging.ListPagingDataAdapter
import indi.yume.tools.dsladapter.paging.RendererLoadStateAdapter
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
import indi.yume.tools.dsladapter.renderers.databinding.CLEAR_ALL
import indi.yume.tools.dsladapter.renderers.databinding.dataBindingItem
import indi.yume.tools.dsladapter.renderers.databinding.databindingOf
import indi.yume.tools.sample.databinding.ItemLayoutBinding
import io.reactivex.Single
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.util.concurrent.TimeUnit

class ListPagingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_paging)

        val recyclerView = findViewById<RecyclerView>(R.id.list_view)

        val service = DataService()

        val flow = Pager(
                PagingConfig(pageSize = 10)
        ) {
            ExamplePagingSource(service)
        }.flow
                .cachedIn(lifecycleScope)

        val pagingAdapter = ListPagingDataAdapter.create(
                itemRenderer = databindingOf<ItemModel>(R.layout.item_layout)
                        .onRecycle(CLEAR_ALL)
                        .itemId(BR.model)
                        .itemId(BR.content, { m -> m.content + "xxxx" })
                        .stableIdForItem { it.id }
                        .forItem(),
                placeholderRenderer = LayoutRenderer.dataBindingItem<Unit, ItemLayoutBinding>(
                        count = 1,
                        layout = R.layout.item_layout,
                        bindBinding = { ItemLayoutBinding.bind(it) },
                        binder = { bind, item, _ ->
                            bind.content = "This is placeholder"
                        },
                        recycleFun = { it.model = null; it.content = null; it.click = null }),
                diffCallback = ItemModelComparator
        )
        val footerAdapter = RendererLoadStateAdapter(databindingOf<LoadState>(R.layout.list_footer)
                .itemId(BR.text) {
                    when (it) {
                        is LoadState.NotLoading -> "NotLoading"
                        is LoadState.Loading -> "Loading"
                        is LoadState.Error -> "Error"
                    }
                }
                .forItem())
        recyclerView.adapter = pagingAdapter.withLoadStateFooter(footerAdapter)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            flow.collectLatest { pagingData ->
                pagingAdapter.submitOriData(pagingData)
            }
        }
    }
}

object ItemModelComparator : DiffUtil.ItemCallback<ItemModel>() {
    override fun areItemsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean {
        // Id is unique.
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean {
        return oldItem == newItem
    }
}

class ExamplePagingSource(
        val service: DataService
) : PagingSource<Int, ItemModel>() {
    override fun getRefreshKey(state: PagingState<Int, ItemModel>): Int? {
        val p = state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
        return p
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ItemModel> {
        val pageNum = params.key ?: 1
        val datas = service.getItems(pageNum).await()
        return LoadResult.Page(
                data = datas,
                prevKey = null,
                nextKey = if (pageNum > 10) null else pageNum + 1
        )
    }

}

class DataService {
    fun getItems(page: Int): Single<List<ItemModel>> =
            Single.timer(2000, TimeUnit.MILLISECONDS)
                    .flatMap { dataSupplier(page) }
}