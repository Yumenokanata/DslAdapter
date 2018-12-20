package indi.yume.tools.dsladapter

import android.support.annotation.CheckResult
import android.support.annotation.MainThread
import android.support.v7.widget.RecyclerView
import android.util.SparseIntArray
import android.view.ViewGroup
import arrow.Kind
import indi.yume.tools.dsladapter.datatype.ActionComposite
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.datatype.dispatchUpdatesTo
import indi.yume.tools.dsladapter.renderers.getEndsPonints
import indi.yume.tools.dsladapter.renderers.resolveIndices
import indi.yume.tools.dsladapter.typeclass.ForViewData
import indi.yume.tools.dsladapter.typeclass.Renderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.typeclass.fix
import java.util.*
import kotlin.collections.ArrayList

typealias Supplier<T> = () -> T

//class OldRendererAdapter<T>(builder: OldBuilder) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//    private val dataLock = Any()
//
//    private val repositoryCount: Int
//    internal val repositories: List<RepoItem<Any>>
//
//
//
//    private var data: List<Kind<ForViewData, *>> = emptyList()
//        set(value) {
//            field = value
//            endPositions = value.getEndsPonints { it.fix().count }
//        }
//    private var endPositions: IntArray = intArrayOf()
//
//    /**
//     * Because the order of function [.getItemViewType] and
//     * [.onCreateViewHolder] calls is uncertain,
//     * only the last Position will be saved for same type.
//     */
//    private val typeToPositionMap = SparseIntArray()
//    private val presenterForViewHolder: WeakHashMap<RecyclerView.ViewHolder, RepoItem<Any>> = WeakHashMap()
//
//    init {
//        repositories = builder.repositories
//        repositoryCount = repositories.size
//
//        data = getCurrentViewData()
//    }
//
//    fun getCurrentViewData(): List<ViewData> =
//            repositories.map {
//                it.renderer.getData(it.supplier())
//            }
//
//    fun getUpdates(oldData: List<ViewData>, newData: List<ViewData>): List<UpdateActions> {
//        val iter1 = repositories.iterator()
//        val iter2 = oldData.iterator()
//        val iter3 = newData.iterator()
//
//        var offset = 0
//        val actionList = ArrayList<UpdateActions>(repositories.size)
//        while (iter1.hasNext() && iter2.hasNext() && iter3.hasNext()) {
//            val repo = iter1.next()
//            val oldItem = iter2.next()
//            val newItem = iter3.next()
//
//            val actions = repo.renderer.getUpdates(oldItem, newItem).filterUselessAction()
//            if (actions.isNotEmpty())
//                actionList += ActionComposite(offset, actions)
//
//            offset += newItem.count
//        }
//
//        return actionList
//    }
//
//    fun getViewData(): List<ViewData> = synchronized(dataLock) { data }
//
//    @CheckResult
//    fun autoUpdateAdapter(): OldUpdateData {
//        val oldData = synchronized(dataLock) { data }
//        val newData = getCurrentViewData()
//
//        return OldUpdateData(
//                oldData = oldData,
//                newData = newData,
//                actions = getUpdates(oldData, newData))
//    }
//
//    @MainThread
//    fun updateData(updates: OldUpdateData) {
//        val dataHasChanged = synchronized(dataLock) {
//            if (data != updates.oldData) {
//                true
//            } else {
//                data = updates.newData
//                false
//            }
//        }
//
//        if (dataHasChanged)
//            forceUpdateAdapter()
//        else
//            updates.actions.dispatchUpdatesTo(this)
//    }
//
//    @MainThread
//    fun forceUpdateAdapter() {
//        val newData = getCurrentViewData()
//        synchronized(dataLock) {
//            data = newData
//        }
//        notifyDataSetChanged()
//    }
//
//    override fun getItemCount(): Int =
//            getViewData().sumBy { it.count }
//
//    override fun getItemViewType(position: Int): Int {
//        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position, endPositions)
//        val type = repositories[resolvedRepositoryIndex].renderer.getItemViewType(
//                data[resolvedRepositoryIndex], resolvedItemIndex)
//        typeToPositionMap.put(type, position)
//        return type
//    }
//
//    override fun getItemId(position: Int): Long {
//        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position, endPositions)
//        val repo = repositories[resolvedRepositoryIndex]
//        return repo.renderer.getItemId(data[resolvedRepositoryIndex], resolvedItemIndex)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup,
//                                    viewType: Int): RecyclerView.ViewHolder {
//        val typeLastPosition = typeToPositionMap.get(viewType)
//
//        val (resolvedRepositoryIndex, _) = resolveIndices(typeLastPosition, endPositions)
//        return repositories[resolvedRepositoryIndex].renderer.onCreateViewHolder(parent, viewType)
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position, endPositions)
//        val repo = repositories[resolvedRepositoryIndex]
//        presenterForViewHolder[holder] = repo
//        repo.renderer.bind(data[resolvedRepositoryIndex], resolvedItemIndex, holder)
//    }
//
//    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
//        recycle(holder)
//        return super.onFailedToRecycleView(holder)
//    }
//
//    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
//        recycle(holder)
//    }
//
//    private fun recycle(holder: RecyclerView.ViewHolder) {
//        val repo = presenterForViewHolder.remove(holder)
//        if (repo != null) {
//            repo.renderer.recycle(holder)
//        }
//    }
//
//    companion object {
//        fun repositoryAdapter(): OldBuilder {
//            return OldBuilder()
//        }
//    }
//}
//
//class OldBuilder internal constructor() {
//    internal val repositories: MutableList<RepoItem<Any>> = ArrayList()
//
//    fun <T, VD : ViewData<T>> add(supplier: Supplier<T>,
//                               renderer: Renderer<T, VD, Updatable<T, VD>>): OldBuilder {
//        val untypedRepository = supplier as Supplier<Any?>
//        repositories.add(RepoItem(untypedRepository, renderer as Renderer<Any?, ViewData<Any?>, Updatable<Any?, ViewData<Any?>>>, false))
//        return this
//    }
//
//    fun <T, VD : ViewData<T>> addItem(item: T,
//                                   renderer: Renderer<T, VD, Updatable<T, VD>>): OldBuilder {
//        repositories.add(RepoItem({ item as Any }, renderer as Renderer<Any, ViewData>, true))
//        return this
//    }
//
//    fun <VD : ViewData> addStaticItem(renderer: Renderer<Unit, VD>): OldBuilder {
//        repositories.add(RepoItem({ Unit }, renderer as Renderer<Any, ViewData>, true))
//        return this
//    }
//
//    fun build(): RendererAdapter {
//        return RendererAdapter(this)
//    }
//}
//
//data class RepoItem<T>(val supplier: Supplier<T>, val renderer: Renderer<T, ViewData>, val isStatic: Boolean)
//
//data class OldUpdateData(val oldData: List<ViewData>,
//                         val newData: List<ViewData>,
//                         val actions: List<UpdateActions>) {
//    fun dispatchUpdatesTo(adapter: RendererAdapter) =
//            adapter.updateData(this)
//}