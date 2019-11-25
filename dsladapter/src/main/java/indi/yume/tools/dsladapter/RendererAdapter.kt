package indi.yume.tools.dsladapter

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.Renderer
import indi.yume.tools.dsladapter.typeclass.ViewData


class RendererAdapter<T, VD : ViewData<T>>(
        val initData: T,
        val renderer: BaseRenderer<T, VD>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val dataLock = Any()

    private var adapterViewData: VD = renderer.getData(initData)

    fun getOriData(): T = adapterViewData.originData

    fun getViewData(): VD = adapterViewData

    @MainThread
    fun setData(newData: T) {
        adapterViewData = renderer.getData(newData)
        notifyDataSetChanged()
    }

    @MainThread
    fun reduceData(f: (T) -> T) {
        val newData = f(adapterViewData.originData)
        setData(newData)
    }

    @MainThread
    fun setViewData(newVD: VD) {
        adapterViewData = newVD
        notifyDataSetChanged()
    }

    @MainThread
    fun reduceViewData(f: (VD) -> VD) {
        val newVD = f(adapterViewData)
        setViewData(newVD)
    }

    /**
     * notifyDataSetChanged()
     */
    @CheckResult
    fun updateAll(newData: T): UpdateResult<T, VD> {
        val oldVD = adapterViewData
        val newVD = renderer.getData(newData)

        return UpdateResult(oldVD, newVD, null)
    }

    @MainThread
    fun updateData(actions: ActionU<VD>) {
        val data = adapterViewData
        val (update, newVD) = actions(data)

        synchronized(dataLock) {
            adapterViewData = newVD
        }

        listOf(update).filterUselessAction().dispatchUpdatesTo(this)
    }

    @MainThread
    fun updateData(updates: UpdateResult<T, VD>) {
        val dataHasChanged = synchronized(dataLock) {
            if (adapterViewData != updates.oldData) {
                adapterViewData = updates.newData
                true
            } else {
                adapterViewData = updates.newData
                false
            }
        }

        if (dataHasChanged || updates.actions == null
                || (updates.oldData != updates.newData && updates.actions.size == 1 && updates.actions.first() is EmptyAction))
            notifyDataSetChanged()
        else
            updates.actions.filterUselessAction().dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = adapterViewData.count

    override fun getItemViewType(position: Int): Int =
            renderer.getItemViewType(adapterViewData, position)

    override fun getItemId(position: Int): Long =
            renderer.getItemId(adapterViewData, position)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder =
            renderer.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            renderer.bind(adapterViewData, position, holder)

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        renderer.recycle(holder)
        return super.onFailedToRecycleView(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        renderer.recycle(holder)
    }

    companion object {
        fun <T, VD : ViewData<T>>
                singleRenderer(initData: T, renderer: BaseRenderer<T, VD>): RendererAdapter<T, VD> =
                RendererAdapter(initData, renderer)

        fun <DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>
                multiple(initData: DL,
                         f: ComposeBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>>.() -> ComposeBuilder<DL, VDL>)
                : RendererAdapter<DL, ComposeViewData<DL, VDL>> =
                RendererAdapter(initData, ComposeRenderer.startBuild.f().build())

        fun multipleBuild(): AdapterBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> =
                AdapterBuilder(HListK.nil(), ComposeRenderer.startBuild)
    }
}


data class UpdateResult<T, VD : ViewData<T>>(val oldData: VD,
                                             val newData: VD,
                                             val actions: List<UpdateActions>?) {
    fun dispatchUpdatesTo(adapter: RendererAdapter<T, VD>) =
            adapter.updateData(this)
}


class AdapterBuilder<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        val initSumData: DL,
        val composeBuilder: ComposeBuilder<DL, VDL>
) {
    fun <T, VD : ViewData<T>>
            add(initData: T, renderer: BaseRenderer<T, VD>)
            : AdapterBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            AdapterBuilder(initSumData.extend(IdT(initData)), composeBuilder.add(renderer))

    fun <VD : ViewData<Unit>>
            add(renderer: BaseRenderer<Unit, VD>)
            : AdapterBuilder<HConsK<ForIdT, Unit, DL>, HConsK<ForComposeItemData, Pair<Unit, VD>, VDL>> =
            AdapterBuilder(initSumData.extend(IdT(Unit)), composeBuilder.add(renderer))

    fun build(): RendererAdapter<DL, ComposeViewData<DL, VDL>> =
            RendererAdapter(initSumData, composeBuilder.build())
}