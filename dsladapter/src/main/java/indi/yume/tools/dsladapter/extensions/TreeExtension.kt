package indi.yume.tools.dsladapter.extensions

import arrow.core.Eval
import arrow.core.Option
import arrow.core.none
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import java.util.*

fun <G, GVD : ViewData<TreeNode.Node<G, L>>,
        L, LVD : ViewData<TreeNode.Leaf<G, L>>> treeAdapter(
        nodeItemRenderer: BaseRenderer<TreeNode.Node<G, L>, GVD>,
        leafItemRenderer: BaseRenderer<TreeNode.Leaf<G, L>, LVD>,
        f: TreeNodeBuilder<G, L>.() -> Unit): RendererAdapter<TreeNode.Root<G, L>, ViewData<TreeNode.Root<G, L>>> =
        RendererAdapter.singleRenderer(TreeNodeBuilder.buildTree(f),
                treeRenderer(nodeItemRenderer, leafItemRenderer))

fun <G, GVD : ViewData<TreeNode.Node<G, L>>,
        L, LVD : ViewData<TreeNode.Leaf<G, L>>> treeRenderer(
        nodeItemRenderer: BaseRenderer<TreeNode.Node<G, L>, GVD>,
        leafItemRenderer: BaseRenderer<TreeNode.Leaf<G, L>, LVD>
): BaseRenderer<TreeNode.Root<G, L>, ViewData<TreeNode.Root<G, L>>> =
        treeBaseRenderer(nodeItemRenderer.toBaseType(), leafItemRenderer.toBaseType())

fun <G, L> treeBaseRenderer(
        nodeItemRenderer: BaseRenderer<TreeNode.Node<G, L>, ViewData<TreeNode.Node<G, L>>>,
        leafItemRenderer: BaseRenderer<TreeNode.Leaf<G, L>, ViewData<TreeNode.Leaf<G, L>>>
): BaseRenderer<TreeNode.Root<G, L>, ViewData<TreeNode.Root<G, L>>> =
        nodeRenderer(nodeItemRenderer, leafItemRenderer)
                .mapT(type<TreeNode.Root<G, L>>(),
                        mapper = { it as TreeNode<G, L> },
                        demapper = { oldData, newMapData ->
                            if (newMapData is TreeNode.Root) newMapData
                            else oldData
                        })
                .toBaseType()


fun <G, GVD : ViewData<TreeNode.Node<G, L>>,
        L, LVD : ViewData<TreeNode.Leaf<G, L>>> nodeRenderer(
        nodeItemRenderer: BaseRenderer<TreeNode.Node<G, L>, GVD>,
        leafItemRenderer: BaseRenderer<TreeNode.Leaf<G, L>, LVD>
): BaseRenderer<TreeNode<G, L>, ViewData<TreeNode<G, L>>> =
        nodeBaseRenderer(nodeItemRenderer.toBaseType(), leafItemRenderer.toBaseType())

fun <G, L> nodeBaseRenderer(
        nodeItemRenderer: BaseRenderer<TreeNode.Node<G, L>, ViewData<TreeNode.Node<G, L>>>,
        leafItemRenderer: BaseRenderer<TreeNode.Leaf<G, L>, ViewData<TreeNode.Leaf<G, L>>>
): BaseRenderer<TreeNode<G, L>, ViewData<TreeNode<G, L>>> {
    lateinit var rootRenderer: MapperRenderer<TreeNode.Root<G, L>, List<TreeNode<G, L>>, ListViewData<List<TreeNode<G, L>>, TreeNode<G, L>, CaseViewData<TreeNode<G, L>>>>
    lateinit var folderRenderer: CaseRenderer<TreeNode.Node<G, L>>

    val itemRenderer = CaseRenderer.build<TreeNode<G, L>> {
        case(checker = { it is TreeNode.Root },
                mapper = { it as TreeNode.Root<G, L> },
                renderer = LazyRenderer { rootRenderer })

        case(checker = { it is TreeNode.Node },
                mapper = { it as TreeNode.Node<G, L> },
                renderer = LazyRenderer { folderRenderer })

        case(checker = { it is TreeNode.Leaf },
                mapper = { it as TreeNode.Leaf<G, L> },
                renderer = leafItemRenderer)
    }

    rootRenderer =
            itemRenderer.forList({ item, i -> "${item.deep}|${item.id}|${item.name}" })
                    .mapT(type<TreeNode.Root<G, L>>(),
                            mapper = { parent -> parent.nodes },
                            demapper = { oldData, newMapData ->
                                oldData.update(nodes = newMapData)
                            })
    folderRenderer = CaseRenderer.build<TreeNode.Node<G, L>> {
        case(checker = { it.isOpen },
                renderer = SplitRenderer.build {
                    add(nodeItemRenderer)
                    add(itemRenderer.forList({ item, i -> "${item.deep}|${item.id}|${item.name}" })
                            .mapT(type<TreeNode.Node<G, L>>(),
                                    mapper = { parent -> parent.nodes.value() },
                                    demapper = { oldData, newMapData ->
                                        oldData.update(nodes = Eval.just(newMapData))
                                    }))
                })

        elseCase(renderer = nodeItemRenderer)
    }

    return itemRenderer.toBaseType()
}

@DslMarker
annotation class TreeBuilder

typealias TreeBuilderFun<G, L> = TreeNodeBuilder<G, L>.() -> Unit

class TreeNodeBuilder<G, L> {
    val nodes: MutableList<TreeBuilderNode<G, L>> = LinkedList()

    @TreeBuilder
    fun addAll(nodeList: List<TreeBuilderNode<G, L>>): TreeNodeBuilder<G, L> {
        nodes.addAll(nodes)
        return this
    }

    @TreeBuilder
    fun addLeaf(name: String, data: L): TreeNodeBuilder<G, L> {
        nodes += TreeBuilderNode.Leaf<G, L>(name = name, data = data)
        return this
    }

    @TreeBuilder
    fun addNode(name: String, data: G, isOpen: Boolean = true, children: List<TreeBuilderNode<G, L>>): TreeNodeBuilder<G, L> {
        nodes += TreeBuilderNode.Node<G, L>(name = name,
                data = data, nodes = Eval.just(children), isOpen = isOpen)
        return this
    }

    @TreeBuilder
    fun addNode(name: String, data: G, isOpen: Boolean = true, childrenF: TreeNodeBuilder<G, L>.() -> Unit): TreeNodeBuilder<G, L> {
        val subNode =

                TreeNodeBuilder<G, L>().apply { childrenF() }
                        .createNode(name, data, isOpen)
        nodes += subNode
        return this
    }

    @TreeBuilder
    fun addNodeLazy(name: String, data: G, isOpen: Boolean = false, children: Eval<List<TreeBuilderNode<G, L>>>): TreeNodeBuilder<G, L> {
        nodes += TreeBuilderNode.Node<G, L>(name = name,
                data = data, nodes = children, isOpen = isOpen)
        return this
    }

    @TreeBuilder
    fun addNodeLazy(name: String, data: G, isOpen: Boolean = false, childrenF: TreeNodeBuilder<G, L>.() -> Unit): TreeNodeBuilder<G, L> {
        val subNode = TreeBuilderNode.Node<G, L>(name = name, data = data, isOpen = isOpen,
                nodes = Eval.later { TreeNodeBuilder<G, L>().apply { childrenF() }.create() })
        nodes += subNode
        return this
    }

    fun create(): List<TreeBuilderNode<G, L>> = nodes.toList()

    fun createNode(name: String, data: G, isOpen: Boolean = true): TreeBuilderNode.Node<G, L> =
            TreeBuilderNode.Node<G, L>(name = name, data = data, nodes = Eval.just(create()), isOpen = isOpen)

    companion object {
        fun <G, L> buildTree(f: TreeNodeBuilder<G, L>.() -> Unit): TreeNode.Root<G, L> =
                build(f).toRealTree()

        fun <G, L> build(f: TreeNodeBuilder<G, L>.() -> Unit): TreeBuilderNode<G, L> {
            val builder = TreeNodeBuilder<G, L>()
            builder.f()
            return TreeBuilderNode.Root(builder.create())
        }
    }
}

fun <G, L> TreeBuilderNode<G, L>.toRealTree(): TreeNode.Root<G, L> {
    val node = toRealNode(0, 0)
    return if (node !is TreeNode.Root) {
        val root = TreeNode.Root<G, L>(listOf(node))
        node.parent = root
        root
    } else node
}

fun <G, L> TreeBuilderNode<G, L>.toRealNode(deep: Int, id: Int): TreeNode<G, L> =
        when (this) {
            is TreeBuilderNode.Root -> TreeNode.Root(nodes.withIndex().map { (index, subItem) ->
                subItem.toRealNode(deep + 1, index)
            })
            is TreeBuilderNode.Leaf -> TreeNode.Leaf(deep = deep, name = name, id = id, data = data)
            is TreeBuilderNode.Node -> {
                lateinit var node: TreeNode.Node<G, L>
                val realNodes = nodes.map {
                    it.withIndex().map { (index, subItem) ->
                        subItem.toRealNode(deep + 1, index)
                    }.onEach { it.parent = node }
                }.memoize()

                node = TreeNode.Node(deep = deep, name = name, id = id, data = data,
                        nodes = realNodes, isOpen = isOpen)

                node
            }
        }

fun <G, L> TreeNode.Root<G, L>.toTargetRealNodes(nodes: List<TreeBuilderNode<G, L>>): List<TreeNode<G, L>> =
        nodes.withIndex().map { (index, item) ->
            item.toRealNode(this@toTargetRealNodes.deep + 1, index)
        }

fun <G, L> TreeNode.Node<G, L>.toTargetRealNodes(nodes: List<TreeBuilderNode<G, L>>): List<TreeNode<G, L>> =
        nodes.withIndex().map { (index, item) ->
            item.toRealNode(this@toTargetRealNodes.deep + 1, index)
        }

fun <G, L> TreeNode.Root<G, L>.toBuilderTree(): TreeBuilderNode.Root<G, L> =
        TreeBuilderNode.Root(nodes.map { subItem ->
            subItem.toBuilderNode()
        })

fun <G, L> TreeNode<G, L>.toBuilderNode(): TreeBuilderNode<G, L> =
        when (this) {
            is TreeNode.Root -> TreeBuilderNode.Root(nodes.map { subItem ->
                subItem.toBuilderNode()
            })
            is TreeNode.Leaf -> TreeBuilderNode.Leaf(name = name, data = data)
            is TreeNode.Node -> TreeBuilderNode.Node(name = name, data = data,
                    nodes = nodes.map { subItem ->
                        subItem.map { it.toBuilderNode() }
                    }, isOpen = isOpen)
        }

sealed class TreeBuilderNode<out G, out L> {
    abstract val name: String

    data class Root<G, L>(val nodes: List<TreeBuilderNode<G, L>> = emptyList()): TreeBuilderNode<G, L>() {
        override val name: String = "root"
    }

    data class Node<G, L>(
            override val name: String,
            val data: G,
            val nodes: Eval<List<TreeBuilderNode<G, L>>>,
            val isOpen: Boolean = true): TreeBuilderNode<G, L>()

    data class Leaf<G, L>(
            override val name: String,
            val data: L): TreeBuilderNode<G, L>()
}


fun <G, L> TreeNode<G, L>.getParentPathString(): String =
        getParentPath().joinToString(separator = "/", prefix = "/", postfix = "") { it.name }

fun <G, L> TreeNode<G, L>.getParentPath(): List<TreeNode<G, L>> =
        sequence {
            var node = this@getParentPath.parent
            while (node !is TreeNode.Root) {
                yield(node)
                node = node.parent
            }
        }.toList().asReversed()

tailrec fun <G, L> TreeNode<G, L>.getRoot(): TreeNode.Root<G, L> =
        when (this) {
            is TreeNode.Root -> this
            else -> getParent().getRoot()
        }

fun <G, L> TreeNode<G, L>.relativePathToByName(relativePathNames: List<String>): TreeNode<G, L>? {
    val baseDeep = deep
    val targetDeep = relativePathNames.size + baseDeep

    fun getTarget(node: TreeNode<G, L>): TreeNode<G, L>? = when(node) {
        is TreeNode.Root -> if (targetDeep == 0) node
        else node.nodes.asSequence().map { getTarget(it) }.filterNotNull().firstOrNull()
        is TreeNode.Node -> if (targetDeep == node.deep)
            if (relativePathNames.getOrNull(node.deep - baseDeep - 1) == node.name) node
            else null
        else node.nodes.value().asSequence().map { getTarget(it) }.filterNotNull().firstOrNull()
        is TreeNode.Leaf -> if (targetDeep == node.deep
                && relativePathNames.getOrNull(node.deep - baseDeep - 1) == node.name)
            node
        else
            null
    }

    return getTarget(this)
}

fun <G, L> TreeNode.Root<G, L>.updateRoot(newNode: TreeNode<G, L>): TreeNode.Root<G, L> {
    val pathList = newNode.getParentPath()
    return update(nodes = nodes.map { it.updateNode(newNode, pathList) })
}

fun <G, L> TreeNode<G, L>.updateFromRoot(newNode: TreeNode<G, L>): TreeNode.Root<G, L> =
        getRoot().updateRoot(newNode)

fun <G, L> TreeNode<G, L>.updateNode(
        newNode: TreeNode<G, L>,
        pathList: List<TreeNode<G, L>> = newNode.getParentPath()): TreeNode<G, L> {
    if (deep == newNode.deep && id == newNode.id) return newNode
    else if (deep < newNode.deep && this is TreeNode.Node) {
        val target = pathList.getOrNull(deep - 1)
        if (target?.id == id && target?.name == name) {
            return update<G, L>(nodes = nodes.map { n -> n.map { it.updateNode(newNode, pathList) } })
        }
    } else if (deep < newNode.deep && this is TreeNode.Root) {
        val target = pathList.getOrNull(deep - 1)
        if (target?.id == id && target?.name == name) {
            return update<G, L>(nodes = nodes.map { it.updateNode(newNode, pathList) })
        }
    }

    return this
}

fun <G, L> TreeNode.Root<G, L>.updateF(
        f: (MutableList<TreeBuilderNode<G, L>>.() -> Unit)? = null): TreeNode.Root<G, L> =
        update(f?.let {
            val oldNodes = nodes.map { it.toBuilderNode() }.toMutableList()
            oldNodes.it()
            this@updateF.toTargetRealNodes(oldNodes)
        })

fun <G, L> TreeNode.Root<G, L>.update(
        nodes: List<TreeNode<G, L>>? = null): TreeNode.Root<G, L> {
    return copy(nodes = nodes ?: this.nodes)
            .apply { parent = this@update.parent }
}

fun <G, L> TreeNode.Node<G, L>.updateF(
        name: String? = null,
        data: Option<G> = none(),
        isOpen: Boolean? = null,
        f: (MutableList<TreeBuilderNode<G, L>>.() -> Unit)? = null): TreeNode.Node<G, L> =
        update(name, data, isOpen, f?.let {
            val oldNodes = nodes.value().map { it.toBuilderNode() }.toMutableList()
            oldNodes.it()
            Eval.just(this@updateF.toTargetRealNodes(oldNodes))
        })

fun <G, L> TreeNode.Node<G, L>.update(
        name: String? = null,
        data: Option<G> = none(),
        isOpen: Boolean? = null,
        nodes: Eval<List<TreeNode<G, L>>>? = null): TreeNode.Node<G, L> {
    return copy(name = name ?: this.name, data = data.orNull() ?: this.data,
            isOpen = isOpen ?: this.isOpen,
            nodes = nodes ?: this.nodes).apply { parent = this@update.parent }
}

fun <G, L> TreeNode.Leaf<G, L>.update(
        name: String? = null,
        data: Option<L> = none()): TreeNode.Leaf<G, L> {
    return copy(name = name ?: this.name, data = data.orNull() ?: this.data)
            .apply { parent = this@update.parent }
}

fun <G, L> TreeNode<G, L>.prettyString(): String =
        StringBuilder().also { prettyStringInner(it) }.toString()

internal fun <G, L> TreeNode<G, L>.prettyStringInner(builder: StringBuilder): Unit = when(this) {
    is TreeNode.Root -> {
        builder.appendln("root")
        nodes.forEach { it.prettyStringInner(builder) }
    }
    is TreeNode.Node -> {
        builder.appendln("${' '.repeat(deep * 2)}|+ ${name}")
        nodes.value().forEach { it.prettyStringInner(builder) }
    }
    is TreeNode.Leaf -> {
        builder.appendln("${' '.repeat(deep * 2)}|- ${name}")
        Unit
    }
}

sealed class TreeNode<G, L> {
    abstract val deep: Int
    abstract val name: String
    abstract val id: Int
    internal abstract var parent: TreeNode<G, L>

    fun getParent(): TreeNode<G, L> = parent

    data class Root<G, L>(val nodes: List<TreeNode<G, L>> = emptyList()): TreeNode<G, L>() {
        override val deep: Int = 0
        override val name: String = "root"
        override val id: Int = 0
        override var parent: TreeNode<G, L> = this

        init {
            nodes.forEach { it.parent = this@Root }
        }
    }

    data class Node<G, L>(
            override val deep: Int,
            override val name: String,
            override val id: Int,
            val data: G,
            val nodes: Eval<List<TreeNode<G, L>>>,
            val isOpen: Boolean = true): TreeNode<G, L>() {
        override lateinit var parent: TreeNode<G, L>
    }

    data class Leaf<G, L>(
            override val deep: Int,
            override val name: String,
            override val id: Int,
            val data: L): TreeNode<G, L>() {
        override lateinit var parent: TreeNode<G, L>
    }
}