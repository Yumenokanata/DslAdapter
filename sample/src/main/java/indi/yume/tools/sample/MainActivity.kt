package indi.yume.tools.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import indi.yume.tools.adapterdatabinding.DO_NOTHING
import indi.yume.tools.adapterdatabinding.databindingOf
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.datatype.dispatchUpdatesTo
import indi.yume.tools.dsladapter.forList
import indi.yume.tools.dsladapter.layout
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
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
                .add({ provideData(index) },
                        LayoutRenderer<ItemModel>(layout = R.layout.simple_item,
                                stableIdForItem = { item, index -> item.id },
                                binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = itemModel.title },
                                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                .forList({ i, index -> index }))
                .add({ provideData(index) },
                        databindingOf<ItemModel>(R.layout.item_layout)
                                .onRecycle(DO_NOTHING)
                                .itemId(BR.model)
                                .itemId(BR.content, { m -> m.content + "xxxx" })
                                .stableIdForItem { it.id }
                                .checkKey { i, index -> index }
                                .forList())
                .addItem(DateFormat.getInstance().format(Date()),
                        databindingOf<String>(R.layout.list_footer)
                                .itemId(BR.text)
                                .forItem())
                .build()
        adapter.setHasStableIds(true)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.load_next_button).setOnClickListener({ v ->
            index++
            Single.fromCallable { adapter.autoUpdateAdapter() }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { it.dispatchUpdatesTo(adapter) })
        })
    }

    private fun provideData(pageIndex: Int): List<ItemModel> {
        return dataSupplier(pageIndex)
                .blockingGet()
    }

    private fun dataSupplier(pageIndex: Int): Single<List<ItemModel>> {
        return Single.just(genList(pageIndex * 10, 10))
                .delay(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
    }
}
