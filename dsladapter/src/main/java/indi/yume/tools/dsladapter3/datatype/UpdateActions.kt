package indi.yume.tools.dsladapter3.datatype

import android.support.v7.util.AdapterListUpdateCallback
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView


sealed class UpdateActions

data class OnInserted(val pos: Int, val count: Int) : UpdateActions()

data class OnRemoved(val pos: Int, val count: Int) : UpdateActions()

data class OnMoved(val fromPosition: Int, val toPosition: Int) : UpdateActions()

data class OnChanged(val pos: Int, val count: Int, val payload: Any?) : UpdateActions()

data class ActionComposite(val offset: Int, val actions: List<UpdateActions>) : UpdateActions()

fun List<UpdateActions>.dispatchUpdatesTo(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
    dispatchUpdatesTo(AdapterListUpdateCallback(adapter))
}

fun List<UpdateActions>.dispatchUpdatesTo(callback: ListUpdateCallback) {
    for (act in this)
        dispatch(0, act, callback)
}

fun dispatch(offset: Int, action: UpdateActions, callback: ListUpdateCallback) {
    when (action) {
        is OnInserted -> callback.onInserted(offset + action.pos, action.count)
        is OnRemoved -> callback.onRemoved(offset + action.pos, action.count)
        is OnMoved -> callback.onMoved(offset + action.fromPosition, offset + action.toPosition)
        is OnChanged -> callback.onChanged(offset + action.pos, action.count, action.payload)
        is ActionComposite -> {
            for(subAct in action.actions)
                dispatch(offset + action.offset, subAct, callback)
        }
    }
}