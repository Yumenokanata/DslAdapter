package indi.yume.tools.dsladapter.typeclass

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
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

    companion object {
        private class Recycler(val f: (RecyclerView.ViewHolder) -> Unit)

        fun RecyclerView.ViewHolder.bindRecycle(any: Any, f: (RecyclerView.ViewHolder) -> Unit) {
            itemView?.setTag(any.hashCode(), Recycler(f))
        }

        fun RecyclerView.ViewHolder.doRecycle(any: Any) {
            val tag = itemView?.tag
            if(tag is Recycler)
                tag.f(this)

            itemView?.tag = null
        }
    }
}