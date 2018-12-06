package indi.yume.tools.sample

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.none
import indi.yume.tools.adapterdatabinding.CLEAR_ALL
import indi.yume.tools.adapterdatabinding.dataBindingItem
import indi.yume.tools.adapterdatabinding.databindingOf
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.forList
import indi.yume.tools.dsladapter.layout
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.sample.databinding.ItemLayoutBinding
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    internal var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val adapter = RendererAdapter.repositoryAdapter()
                .addStaticItem(layout<Unit>(R.layout.list_header))
                .add({ none<List<Option<ItemModel>>>() },
                        optionRenderer(
                                noneItemRenderer = LayoutRenderer.dataBindingItem<Unit, ItemLayoutBinding>(
                                        count = 5,
                                        layout = R.layout.item_layout,
                                        bindBinding = { ItemLayoutBinding.bind(it) },
                                        binder = { bind, item, _ ->
                                            bind.content = "this is empty item"
                                        },
                                        recycleFun = { it.model = null; it.content = null; it.click = null }),
                                itemRenderer = LayoutRenderer.dataBindingItem<Option<ItemModel>, ItemLayoutBinding>(
                                        count = 5,
                                        layout = R.layout.item_layout,
                                        bindBinding = { ItemLayoutBinding.bind(it) },
                                        binder = { bind, item, _ ->
                                            bind.content = "this is empty item"
                                        },
                                        recycleFun = { it.model = null; it.content = null; it.click = null })
                                        .forList()
                        ))
                .add({ provideData(index) },
                        ComposeRenderer.startBuild {
                            plus({ it },
                                    LayoutRenderer<ItemModel>(layout = R.layout.simple_item,
                                            stableIdForItem = { item, index -> item.id },
                                            binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = itemModel.title },
                                            recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                            .forList({ i, index -> index }))
                            plus(databindingOf<ItemModel>(R.layout.item_layout)
                                            .onRecycle(CLEAR_ALL)
                                            .itemId(BR.model)
                                            .itemId(BR.content, { m -> m.content + "xxxx" })
                                            .stableIdForItem { it.id }
                                            .checkKey { i, index -> index }
                                            .forList())
                        })
                .add({ provideData(index) },
                        LayoutRenderer<ItemModel>(layout = R.layout.simple_item,
                                stableIdForItem = { item, index -> item.id },
                                binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = itemModel.title },
                                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                .forList({ i, index -> index }))
                .add({ provideData(index) },
                        GroupItemRenderer(
                                groupGetter = { "title" },
                                subsGetter = { it },
                                group = LayoutRenderer<String>(layout = R.layout.simple_item,
                                        count = 3,
                                        binder = { view, title, index -> view.findViewById<TextView>(R.id.simple_text_view).text = title + index },
                                        recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" }),
                                subs = databindingOf<ItemModel>(R.layout.item_layout)
                                        .onRecycle(CLEAR_ALL)
                                        .itemId(BR.model)
                                        .itemId(BR.content, { m -> m.content + "xxxx" })
                                        .stableIdForItem { it.id }
                                        .checkKey { i, index -> index }
                                        .forItem()))
                .addItem(DateFormat.getInstance().format(Date()),
                        databindingOf<String>(R.layout.list_footer)
                                .itemId(BR.text)
                                .forItem())
                .build()

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.load_next_button).setOnClickListener({ v ->
            index++
            Single.fromCallable { adapter.autoUpdateAdapter() }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { adapter.updateData(it) })
        })
    }

    private fun provideData(pageIndex: Int): List<ItemModel> {
        return dataSupplier(pageIndex)
                .blockingGet()
    }

    private fun dataSupplier(pageIndex: Int): Single<List<ItemModel>> {
        return Single.just(genList(pageIndex * 10, 10 - pageIndex % 10))
                .delay(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
    }
}


fun <T, NVD : ViewData, SVD : ViewData> optionRenderer(noneItemRenderer: BaseRenderer<Unit, NVD>,
                                                       itemRenderer: BaseRenderer<T, SVD>): SealedItemRenderer<Option<T>> =
        SealedItemRenderer<Option<T>>(listOf(
                item({ it is None }, { Unit }, noneItemRenderer),

                item({ it is Some }, { it.orNull()!! }, itemRenderer))
        )
