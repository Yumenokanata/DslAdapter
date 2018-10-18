package indi.yume.tools.dsladapter.typeclass

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

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

        private class RecyclerMap(map: Map<Int, Recycler> = emptyMap()): Map<Int, Recycler> by map

        fun RecyclerView.ViewHolder.bindRecycle(any: Any, f: (RecyclerView.ViewHolder) -> Unit) {
            itemView?.apply {
                val tag = tag
                if (tag != null && tag is RecyclerMap)
                    setTag(RecyclerMap(tag + (any.hashCode() to Recycler(f))))
                else
                    setTag(RecyclerMap(mapOf(any.hashCode() to Recycler(f))))
            }
        }

        fun RecyclerView.ViewHolder.doRecycle(any: Any) {
            val tag = itemView?.tag
            val key = any.hashCode()
            itemView?.tag = if(tag is RecyclerMap)
                tag.get(key)?.let {
                    it.f(this)
                    RecyclerMap(tag - key)
                } ?: tag
            else
                tag
        }
    }
}