package indi.yume.tools.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.renderers.databinding.CLEAR_ALL
import indi.yume.tools.dsladapter.renderers.databinding.databindingOf
import indi.yume.tools.dsladapter.updater.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

class ComposeActivity : AppCompatActivity() {

    internal var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose)

        val recyclerView = findViewById<RecyclerView>(R.id.compose_recycler_view)

        val stringRenderer = LayoutRenderer<String>(layout = R.layout.simple_item,
                count = 3,
                binder = { view, title, index -> view.findViewById<TextView>(R.id.simple_text_view).text = title + index },
                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })

        val itemRenderer = databindingOf<ItemModel>(R.layout.item_layout)
                .onRecycle(CLEAR_ALL)
                .itemId(BR.model)
                .itemId(BR.content, { m -> m.content + "xxxx" })
                .stableIdForItem { it.id }
                .forItem()

        val renderer = TitleItemRenderer(
                itemType = type<List<ItemModel>>(),
                titleGetter = { "TitleItemRenderer: title" },
                subsGetter = { it },
                title = stringRenderer,
                subs = itemRenderer)

        val adapter = RendererAdapter.multipleBuild()
//                .add(layout<Unit>(R.layout.list_header))
//                .add(listOf(ItemModel().some()).some(),
//                        optionRenderer(
//                                noneItemRenderer = LayoutRenderer.dataBindingItem<Unit, ItemLayoutBinding>(
//                                        count = 5,
//                                        layout = R.layout.item_layout,
//                                        bindBinding = { ItemLayoutBinding.bind(it) },
//                                        binder = { bind, item, _ ->
//                                            bind.content = "Last5 this is empty item"
//                                        },
//                                        recycleFun = { it.model = null; it.content = null; it.click = null }),
//                                itemRenderer = LayoutRenderer.dataBindingItem<Option<ItemModel>, ItemLayoutBinding>(
//                                        count = 5,
//                                        layout = R.layout.item_layout,
//                                        bindBinding = { ItemLayoutBinding.bind(it) },
//                                        binder = { bind, item, _ ->
//                                            bind.content = "Last5 this is some item"
//                                        },
//                                        recycleFun = { it.model = null; it.content = null; it.click = null })
//                                        .forList()
//                        ))
//                .add(provideData(index).let { HListK.singleId(it).putF(it) },
//                        ComposeRenderer.startBuild
//                                .add(LayoutRenderer<ItemModel>(layout = R.layout.simple_item,
//                                        stableIdForItem = { item, index -> item.id },
//                                        binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = "Last4 ${itemModel.title}" },
//                                        recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
//                                        .forList({ i, index -> index }))
//                                .add(databindingOf<ItemModel>(R.layout.item_layout)
//                                        .onRecycle(CLEAR_ALL)
//                                        .itemId(BR.model)
//                                        .itemId(BR.content, { m -> "Last4 " + m.content + "xxxx" })
//                                        .stableIdForItem { it.id }
//                                        .forItem()
//                                        .replaceUpdate { CustomUpdate(it) }
//                                        .forList())
//                                .build().ignoreType
//                                { oldData, newData, payload ->
//                                    getLast1().reduce { data ->
//                                        subs(2) {
//                                            up.update(ItemModel())
//                                        }
//                                    }
//                                })
                .add(provideData(index),
                        LayoutRenderer<ItemModel>(
                                count = 1,
                                layout = R.layout.simple_item,
                                stableIdForItem = { item, index -> item.id },
                                binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = "Last3 ${itemModel.title}" },
                                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                .forList(keyGetter = { i, index -> index }, itemIsSingle = true))
//                .add(provideData(index), renderer)
//                .add(DateFormat.getInstance().format(Date()),
//                        databindingOf<String>(R.layout.list_footer)
//                                .itemId(BR.text)
//                                .forItem())
                .build()

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        var time = 0L
        findViewById<View>(R.id.load_next_button).setOnClickListener { v ->
            index++
            val newData = provideData(index)
            Single.fromCallable {
                time = System.currentTimeMillis()
                val r = adapter.update(::ComposeUpdater) {
//                    getLast2().up {
//                        update(newData)
//                    } +
                            getLast0().up(::ListUpdater) {
                                //                        move(2, 4) +
//                        subs(3) {
//                            update(ItemModel(189, "Subs Title $index", "subs Content"))
//                        }
                                updateAuto(newData.shuffled(), diffUtilCheck { i, _ -> i.id })
                            }
//                    } + getLast4().up {
//                        update(hlistKOf(emptyList<ItemModel>().toIdT(), emptyList<ItemModel>().toIdT()))
//                    } + getLast5().up {
//                        sealedItem({ get0().fix() }) {
//                            update(listOf(ItemModel().some(), none<ItemModel>()))
//                        }
//                    }
                }
                println("computation over: ${System.currentTimeMillis() - time}")
                r
            }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { adapter.updateData(it) })
        }
    }
}

