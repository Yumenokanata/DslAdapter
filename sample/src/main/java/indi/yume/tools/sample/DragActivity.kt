package indi.yume.tools.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.renderers.databinding.databindingOf
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.forList
import indi.yume.tools.dsladapter.layout
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
import java.text.DateFormat
import java.util.*

class DragActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drag)

        val recyclerView = findViewById<RecyclerView>(R.id.drag_recycler_view)

        val adapter = RendererAdapter.multipleBuild()
                .add(layout<Unit>(R.layout.list_header))
                .add(provideData(0),
                        LayoutRenderer<ItemModel>(
                                count = 2,
                                layout = R.layout.simple_item,
                                stableIdForItem = { item, index -> item.id },
                                binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = "Last3 ${itemModel.title}" },
                                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                .forList({ i, index -> index }))
                .add(DateFormat.getInstance().format(Date()),
                        databindingOf<String>(R.layout.list_footer)
                                .itemId(BR.text)
                                .forItem())
                .build()

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)


        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                viewHolder.adapterPosition

                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                        ItemTouchHelper.START or ItemTouchHelper.END)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isLongPressDragEnabled(): Boolean  = true

            override fun isItemViewSwipeEnabled(): Boolean = true
        })
    }
}
