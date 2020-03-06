package indi.yume.tools.dsladapter.typeclass

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import arrow.Kind

interface Renderer<Data, VD : ViewData<Data>> {
    fun getData(content: Data): VD

    fun getItemId(data: VD, index: Int): Long = RecyclerView.NO_ID

    fun getItemViewType(data: VD, position: Int): Int

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    fun bind(data: VD, index: Int, holder: RecyclerView.ViewHolder)

    fun recycle(holder: RecyclerView.ViewHolder)
}

interface ViewData<out OriD> : ViewDataOf<OriD> {
    val count: Int

    val originData: OriD
}

class ForViewData private constructor() { companion object }
typealias ViewDataOf<T> = Kind<ForViewData, T>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> ViewDataOf<T>.fix(): ViewData<T> =
        this as ViewData<T>


/**
 * Use for demapper, if you update mapper target is not affect the Original Data
 */
fun <T, D> doNotAffectOriData(): (T, D) -> T = { oldOriData, newMapData -> oldOriData }

fun <T> awaysReturnNewData(): (T, T) -> T = { oldOriData, newMapData -> newMapData }