package indi.yume.tools.dsladapter3.typeclass

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

abstract class BaseRenderer<T, VD: ViewData> : Renderer<T, VD> {

    override fun getItemViewType(data: VD, position: Int): Int = getLayoutResId(data, position)

    override fun getItemId(data: VD, index: Int): Long {
        return -1L
    }

    @LayoutRes
    abstract fun getLayoutResId(data: VD, position: Int): Int


    override fun recycle(holder: RecyclerView.ViewHolder) {}

    override fun onCreateViewHolder(parent: ViewGroup, layoutResourceId: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layoutResourceId, parent, false)) {

        }
    }
}