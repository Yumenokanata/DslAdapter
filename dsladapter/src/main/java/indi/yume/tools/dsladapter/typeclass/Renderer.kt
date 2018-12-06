package indi.yume.tools.dsladapter.typeclass

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.UpdateActions

interface Renderer<Data, VD: ViewData> {
    fun getData(content: Data): VD

    fun getItemId(data: VD, index: Int): Long = RecyclerView.NO_ID

    fun getItemViewType(data: VD, position: Int): Int

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    fun bind(data: VD, index: Int, holder: RecyclerView.ViewHolder)

    fun recycle(holder: RecyclerView.ViewHolder)

    fun getUpdates(oldData: VD, newData: VD): List<UpdateActions>
}

interface ViewData {
    val count: Int
}