package indi.yume.tools.sample

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.*
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.renderers.databinding.CLEAR_ALL
import indi.yume.tools.dsladapter.renderers.databinding.dataBindingItem
import indi.yume.tools.dsladapter.renderers.databinding.databindingOf
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.forList
import indi.yume.tools.dsladapter.layout
import indi.yume.tools.dsladapter.putF
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.rx2.rxBuild
import indi.yume.tools.dsladapter.rx2.singleRxAutoUpdate
import indi.yume.tools.dsladapter.singleId
import indi.yume.tools.dsladapter.singleSupplier
import indi.yume.tools.dsladapter.supplierBuilder
import indi.yume.tools.dsladapter.toIdT
import indi.yume.tools.dsladapter.type
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.typeclass.doNotAffectOriData
import indi.yume.tools.dsladapter.updater.*
import indi.yume.tools.sample.databinding.ItemLayoutBinding
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    internal var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle("Multiple compose Demo")

        val random = Random()

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        // Part1 Make Renderer

        // 1.1 LayoutRenderer
        val stringRenderer = LayoutRenderer<String>(layout = R.layout.simple_item,
                count = 3,
                binder = { view, title, index -> view.findViewById<TextView>(R.id.simple_text_view).text = title + index },
                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })

        // 1.2 DataBindingRenderer
        val itemRenderer = databindingOf<ItemModel>(R.layout.item_layout)
                .onRecycle(CLEAR_ALL)
                .itemId(BR.model)
                .itemId(BR.content, { m -> m.content + "xxxx" })
                .stableIdForItem { it.id }
                .forItem()

        // 1.3 TitleItemRenderer
        /**
         * This Renderer will build a title with subs, like:
         *
         * |-- Title  (eg: stringRenderer)
         * |-- Sub1   (eg: itemRenderer)
         * |-- Sub2
         * |-- ...
         */
        val renderer = TitleItemRenderer(
                itemType = type<List<ItemModel>>(),
                titleGetter = { "TitleItemRenderer: title" },
                subsGetter = { it },
                title = stringRenderer,
                subs = itemRenderer)

        // 1.4 SealedItemRenderer
        /**
         * This Renderer will build a sealed Renderer, if checker is 'right' will show this renderer, eg:
         *
         * when {
         *   list.isEmpty -> stringRenderer
         *   list.isNotEmpty -> list of itemRenderer
         * }
         */
        val hlist = hlistKOf(
            item<List<ItemModel>, _, _>(type = type(),
                checker = { it.isEmpty() },
                mapper = { "empty" },
                demapper = doNotAffectOriData(),
                renderer = stringRenderer
            ),
            item(type = type<List<ItemModel>>(),
                checker = { it.isNotEmpty() },
                mapper = { it },
                demapper = { oldData, newSubData -> newSubData },
                renderer = itemRenderer.forList()
            )
        )
        val rendererSealed = SealedItemRenderer(hlist)

        // 1.5 ComposeRenderer
        /**
         * This Renderer will compose all item renderer, eg:
         *
         * |-- item1 (eg: itemRenderer)
         * |-- item2 (eg: stringRenderer)
         */
        val composeRenderer = ComposeRenderer.startBuild
                .add(itemRenderer)
                .add(stringRenderer)
                .build()

        val act1 = composeRenderer.updater.updateBy {
                    getLast0().up(::updatable) {
                        autoUpdate("")
                    }
                }

        val act2 = composeRenderer.updater.updateBy {
                    getLast1().up(::updatable) {
                        autoUpdate(ItemModel())
                    }
                }

        // Part2 BuildAdapter
        /**
         * Tips: RendererAdapter only have one renderer
         */

        // 2.1 By constructor
        val adapterDemo1 = RendererAdapter(
                initData = HListK.singleId(emptyList<ItemModel>()).putF("ss"),
                renderer = ComposeRenderer.startBuild
                        .add(renderer)
                        .add(stringRenderer)
                        .build()
        )

        // 2.2 By singleRenderer func
        val adapterDemo2 = RendererAdapter.singleRenderer(
                initData = HListK.singleId(emptyList<ItemModel>()).putF("ss"),
                renderer = ComposeRenderer.startBuild
                        .add(renderer)
                        .add(stringRenderer)
                        .build()
        )

        // 2.3 By multiple func
        val adapterDemo3 = RendererAdapter.multiple(HListK.singleId(emptyList<ItemModel>()).putF("ss"))
        {
            start
                    .add(rendererSealed)
                    .add(stringRenderer)
        }

        // 2.4 By multiple Builder
        val adapterDemo4 = RendererAdapter.multipleBuild()
                .add(emptyList<ItemModel>(), rendererSealed)
                .add("ss", stringRenderer)
                .build()

        // Part3 Update Data

        /**
         * 3.1 Simple method is setData()
         */
        adapterDemo1.setData(HListK.singleId(listOf(ItemModel())).putF("ss2"))

        /**
         * 3.2 Or use reduce method
         */
        adapterDemo1.reduceData { oldData -> oldData.map0 { "ss3".toIdT() } }

        // Two way to part update data:
        /**
         * 3.3 By update() func, this function will return a UpdateResult, this time not really update data for adapter.
         *     please use [dispatchUpdatesTo()] to apply update action to adapter
         */
        adapterDemo1.update(::updatable) {
            getLast1().up(::updatable) {
                title(::updatable) {
                    autoUpdate("new Title-${random.nextInt()}")
                }
            }
        }.dispatchUpdatesTo(adapterDemo1)

        /**
         * 3.4 By updateNow() func
         *     Unlike the update method, this method will apply the update directly to the Adapter.
         */
        adapterDemo1.updateNow(::updatable) {
            getLast1().up(::updatable) {
                title(::updatable) {
                    autoUpdate("new Title-${random.nextInt()}")
                }
            }
        }

        // Part 4 Part Update DSL
        val adapter = RendererAdapter.multipleBuild()
                .add(layout<Unit>(R.layout.list_header))
                .add(listOf(ItemModel().some()).some(),
                        optionRenderer(
                                noneItemRenderer = LayoutRenderer.dataBindingItem<Unit, ItemLayoutBinding>(
                                        count = 5,
                                        layout = R.layout.item_layout,
                                        bindBinding = { ItemLayoutBinding.bind(it) },
                                        binder = { bind, item, _ ->
                                            bind.content = "Last4 this is empty item"
                                        },
                                        recycleFun = { it.model = null; it.content = null; it.click = null }),
                                itemRenderer = LayoutRenderer.dataBindingItem<Option<ItemModel>, ItemLayoutBinding>(
                                        count = 5,
                                        layout = R.layout.item_layout,
                                        bindBinding = { ItemLayoutBinding.bind(it) },
                                        binder = { bind, item, _ ->
                                            bind.content = "Last4 this is some item"
                                        },
                                        recycleFun = { it.model = null; it.content = null; it.click = null })
                                        .forList()
                        ))
                .add(provideData(index).let { HListK.singleId(it).putF(it) },
                        ComposeRenderer.startBuild
                                .add(LayoutRenderer<ItemModel>(layout = R.layout.simple_item,
                                        stableIdForItem = { item, index -> item.id },
                                        binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = "Last3 ${itemModel.title}" },
                                        recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                        .forList({ i, index -> index }))
                                .add(databindingOf<ItemModel>(R.layout.item_layout)
                                        .onRecycle(CLEAR_ALL)
                                        .itemId(BR.model)
                                        .itemId(BR.content, { m -> "Last4 " + m.content + "xxxx" })
                                        .stableIdForItem { it.id }
                                        .forList())
                                .build())
                .add(provideData(index),
                        LayoutRenderer<ItemModel>(
                                count = 2,
                                layout = R.layout.simple_item,
                                stableIdForItem = { item, index -> item.id },
                                binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = "Last2 ${itemModel.title}" },
                                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                .forList({ i, index -> index }))
                .add(provideData(index), renderer)
                .add(DateFormat.getInstance().format(Date()),
                        databindingOf<String>(R.layout.list_footer)
                                .itemId(BR.text)
                                .forItem())
                .build()

        findViewById<View>(R.id.load_next_button).setOnClickListener { v ->
            index++
            val newData = provideData(index)
            Single.fromCallable {
                adapter.update(::updatable) {
                    getLast1().up(::updatable) {
                        autoUpdate(newData)
                    } + getLast2().up(::updatable) {
                        //                        move(2, 4) +
//                        subs(3) {
//                            update(ItemModel(189, "Subs Title $index", "subs Content"))
//                        }
                        updateAuto(newData.shuffled(), diffUtilCheck { i, _ -> i.id })
                    } + getLast3().up(::updatable) {
                        getLast1().reduce(::updatable) { oldData ->
                            autoUpdate(oldData + ItemModel())
                        }
                    } + getLast4().up(::updatable) {
                        sealedItem({ get0().fix() }, ::updatable) {
                            autoUpdate(listOf(ItemModel().some(), none<ItemModel>()))
                        }
                    }
                }
            }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { adapter.updateData(it) })
        }

        // Part 5 Supplier update
        val (adapter3, controller1) = RendererAdapter
                .singleSupplier({ provideData(index) }, renderer)

        val (adapter2, controller) = RendererAdapter.supplierBuilder()
                .addStatic(layout<Unit>(R.layout.list_header))
                .add({ provideData(index) }, renderer)
                .build()

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

//        findViewById<View>(R.id.load_next_button).setOnClickListener { v ->
//            index++
//            controller.updateAll()
//        }


        // Part 6 Rx auto update
        val dataProvider = PublishSubject.create<List<ItemModel>>()
        val dataProvider2 = PublishSubject.create<List<ItemModel>>()

        RendererAdapter.rxBuild()
                .addStatic(layout<Unit>(R.layout.list_header))
                .add(provideData(index), dataProvider2, renderer)
                .add(dataProvider, rendererSealed)
                .buildAutoUpdate { adapter ->
                    //                    recyclerView.adapter = adapter
//                    recyclerView.layoutManager = LinearLayoutManager(this)
                }
                .subscribe()

        dataProvider.onNext(provideData(index))

        RendererAdapter.singleRxAutoUpdate(dataProvider, renderer)
        { adapter ->
            //            recyclerView.adapter = adapter
//            recyclerView.layoutManager = LinearLayoutManager(this)
        }.subscribe()

//        findViewById<View>(R.id.load_next_button).setOnClickListener { v ->
//            index++
//            dataProvider.onNext(provideData(index))
//        }

        // Part 7 Rx part update
        val dataProvider3 = PublishSubject.create<List<ItemModel>>()

        dataProvider3
                .updateTo(adapter)
                {
                    updater.getLast4().up(::updatable) {
                        sealedItem({ get0().fix() }, ::updatable) {
                            autoUpdate(listOf(ItemModel().some(), none<ItemModel>()))
                        }
                    }
                }.subscribe()
    }
}

fun <T, P, VD : ViewData<P>> Observable<T>.updateTo(
        adapter: RendererAdapter<P, VD>,
        updater: BaseRenderer<P, VD>.(newData: T) -> ActionU<VD>): Completable =
        distinctUntilChanged().concatMap {
            observeOn(Schedulers.computation())
                    .map { newData -> adapter.update { updater(newData) } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        it.dispatchUpdatesTo(adapter)
                    }
        }.ignoreElements()
