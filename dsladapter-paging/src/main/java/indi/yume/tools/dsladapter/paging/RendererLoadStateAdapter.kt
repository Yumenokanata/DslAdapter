package indi.yume.tools.dsladapter.paging

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

class RendererLoadStateAdapter<VD : ViewData<LoadState>>(
        val renderer: BaseRenderer<LoadState, VD>
) : LoadStateAdapter<RecyclerView.ViewHolder>() {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, loadState: LoadState) {
        return renderer.bind(renderer.getData(loadState), 0, holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): RecyclerView.ViewHolder {
        return renderer.onCreateViewHolder(parent, renderer.getItemViewType(renderer.getData(loadState), 0))
    }
}