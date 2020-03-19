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
import indi.yume.tools.dsladapter.renderers.ListViewData
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updater.updateNow
import indi.yume.tools.dsladapter.updater.updater
import java.text.DateFormat
import java.util.*

class DragActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drag)
        setTitle("DragHelper Demo")

        val recyclerView = findViewById<RecyclerView>(R.id.drag_recycler_view)

        val adapter =
                RendererAdapter.singleRenderer(provideData(0),
                        LayoutRenderer<ItemModel>(
                                layout = R.layout.drag_item_layout,
                                stableIdForItem = { item, index -> item.id },
                                binder = { view, itemModel, index ->
                                    view.findViewById<TextView>(R.id.simple_text_view).text = "Last3 ${itemModel.title}"
                                },
                                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                .forList({ i, index -> index }))
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dragHelper = DragHelper(adapter)
        val itemTouchHelper = ItemTouchHelper(dragHelper)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }


    class DragHelper<T, VD : ViewData<T>>(
            val adapter: RendererAdapter<List<T>, ListViewData<List<T>, T, VD>>
    ) : ItemTouchHelper.Callback() {
        var hasMoved = false

        var onClearView: (() -> Unit)? = null
        var onDraing: ((Boolean) -> Unit)? = null
        var onUpdate: ((List<T>) -> Unit)? = null

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            onClearView?.invoke()
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val viewHolderPosition = viewHolder.adapterPosition
            val targetPosition = target.adapterPosition

            adapter.updateNow {
                hasMoved = true
                if (viewHolderPosition < targetPosition)
                    updater.move(viewHolderPosition, targetPosition)
                else
                    updater.move(targetPosition, viewHolderPosition)
            }
            onUpdate?.invoke(adapter.getOriData())
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            adapter.updateNow {
                updater.remove(viewHolder.getAdapterPosition(), 1)
            }
            onUpdate?.invoke(adapter.getOriData())
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            onDraing?.invoke(actionState == ItemTouchHelper.ACTION_STATE_DRAG)
        }

    }
}
