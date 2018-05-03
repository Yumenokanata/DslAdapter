package indi.yume.tools.dsladapter3

import android.support.v7.widget.RecyclerView
import android.util.SparseIntArray
import android.view.ViewGroup
import indi.yume.tools.dsladapter3.datatype.ActionComposite
import indi.yume.tools.dsladapter3.datatype.UpdateActions
import indi.yume.tools.dsladapter3.datatype.dispatchUpdatesTo
import indi.yume.tools.dsladapter3.renderers.getEndsPonints
import indi.yume.tools.dsladapter3.renderers.resolveIndices
import indi.yume.tools.dsladapter3.typeclass.Renderer
import indi.yume.tools.dsladapter3.typeclass.ViewData
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

typealias Supplier<T> = () -> T

class RendererAdapter(builder: Builder) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val dataLock = Any()

    private val repositoryCount: Int
    internal val repositories: List<Repo<Any>>

    private lateinit var data: List<ViewData>
    private lateinit var endPositions: IntArray

    /**
     * Because the order of function [.getItemViewType] and
     * [.onCreateViewHolder] calls is uncertain,
     * only the last Position will be saved for same type.
     */
    private val typeToPositionMap = SparseIntArray()
    private val presenterForViewHolder: MutableMap<RecyclerView.ViewHolder, Repo<Any>> = HashMap()

    class Builder private constructor() {
        internal val repositories: MutableList<Repo<Any>> = ArrayList()

        fun <T, VD : ViewData> add(supplier: Supplier<T>,
                                   renderer: Renderer<T, VD>): Builder {
            val untypedRepository = supplier as Supplier<Any>
            repositories.add(Repo(untypedRepository, renderer as Renderer<Any, ViewData>, false))
            return this
        }

        fun <T, VD : ViewData> addItem(item: T,
                                       renderer: Renderer<T, VD>): Builder {
            repositories.add(Repo({ item as Any }, renderer as Renderer<Any, ViewData>, true))
            return this
        }

        fun <VD : ViewData> addStaticItem(renderer: Renderer<Unit, VD>): Builder {
            repositories.add(Repo({ Unit }, renderer as Renderer<Any, ViewData>, true))
            return this
        }

        fun build(): RendererAdapter {
            return RendererAdapter(this)
        }
    }

    init {
        repositories = builder.repositories
        repositoryCount = repositories.size
    }

    fun getCurrentViewData(): List<ViewData> =
            repositories.map {
                it.renderer.getData(it.supplier())
            }

    fun getUpdates(): List<UpdateActions> {
        val oldData = synchronized(dataLock) { data }
        val newData = getCurrentViewData()

        val iter1 = repositories.iterator()
        val iter2 = oldData.iterator()
        val iter3 = newData.iterator()

        var offset = 0
        val actionList = ArrayList<UpdateActions>(repositories.size)
        while (iter1.hasNext() && iter2.hasNext() && iter3.hasNext()) {
            val repo = iter1.next()
            val oldItem = iter2.next()
            val newItem = iter3.next()

            actionList += ActionComposite(offset, repo.renderer.getUpdates(oldItem, newItem))

            offset += newItem.count
        }

        return actionList
    }

    fun getViewData(): List<ViewData> = synchronized(dataLock) { data }

    fun updateData() {
        val newData = getCurrentViewData()

        val ends = newData.getEndsPonints()
        synchronized(dataLock) {
            data = newData
            endPositions = ends
        }
    }

    fun autoUpdateAdapter() {
        updateData()
        getUpdates().dispatchUpdatesTo(this)
    }

    fun forceUpdateAdapter() {
        updateData()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int =
            getViewData().sumBy { it.count }

    override fun getItemViewType(position: Int): Int {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position, endPositions)
        val type = repositories[resolvedRepositoryIndex].renderer.getItemViewType(
                data[resolvedRepositoryIndex], resolvedItemIndex)
        typeToPositionMap.put(type, position)
        return type
    }

    override fun getItemId(position: Int): Long {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position, endPositions)
        val repo = repositories[resolvedRepositoryIndex]
        return repo.renderer.getItemId(data[resolvedRepositoryIndex], resolvedItemIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder {
        val typeLastPosition = typeToPositionMap.get(viewType)

        val (resolvedRepositoryIndex, _) = resolveIndices(typeLastPosition, endPositions)
        return repositories[resolvedRepositoryIndex].renderer.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (resolvedRepositoryIndex, resolvedItemIndex) = resolveIndices(position, endPositions)
        val repo = repositories[resolvedRepositoryIndex]
        presenterForViewHolder[holder] = repo
        repo.renderer.bind(data[resolvedRepositoryIndex], resolvedItemIndex, holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        recycle(holder)
        return super.onFailedToRecycleView(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        recycle(holder)
    }

    private fun recycle(holder: RecyclerView.ViewHolder) {
        val repo = presenterForViewHolder.remove(holder)
        if (repo != null) {
            repo.renderer.recycle(holder)
        }
    }
}

data class Repo<T>(val supplier: Supplier<T>, val renderer: Renderer<T, ViewData>, val isStatic: Boolean)