package indi.yume.tools.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Eval
import arrow.core.Option
import arrow.core.none
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.extensions.*
import indi.yume.tools.dsladapter.forList
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.renderers.databinding.CLEAR_ALL
import indi.yume.tools.dsladapter.renderers.databinding.dataBindingItem
import indi.yume.tools.dsladapter.renderers.databinding.databindingOf
import indi.yume.tools.dsladapter.toBaseType
import indi.yume.tools.dsladapter.type
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.sample.databinding.TreeFileItemLayoutBinding
import indi.yume.tools.sample.databinding.TreeFolderItemLayoutBinding
import java.util.*

class TreeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree)
        setTitle("Tree util Demo")

        val recyclerView = findViewById<RecyclerView>(R.id.folder_tree_recycler_view)

        lateinit var adapter: RendererAdapter<TreeNode.Root<FolderNode.Folder, FolderNode.File>, ViewData<TreeNode.Root<FolderNode.Folder, FolderNode.File>>>
        val fileRenderer =
                LayoutRenderer.dataBindingItem<TreeNode.Leaf<FolderNode.Folder, FolderNode.File>, TreeFileItemLayoutBinding>(
                        layout = R.layout.tree_file_item_layout,
                        bindBinding = { TreeFileItemLayoutBinding.bind(it) },
                        binder = { bind, item, _ ->
                            bind.name = item.name
                            bind.leftAnchor.setLeftMarginDp(16 * item.deep)
                        },
                        recycleFun = { bind ->
                            bind.name = null
                            bind.leftAnchor.setLeftMarginDp(0)
                        })
        val folderItemRenderer =
                LayoutRenderer.dataBindingItem<TreeNode.Node<FolderNode.Folder, FolderNode.File>, TreeFolderItemLayoutBinding>(
                        layout = R.layout.tree_folder_item_layout,
                        bindBinding = { TreeFolderItemLayoutBinding.bind(it) },
                        binder = { bind, item, _ ->
                            bind.name = item.name
                            bind.isOpen = item.isOpen
                            bind.leftAnchor.setLeftMarginDp(16 * item.deep)
                            bind.containerLayout.setOnClickListener {
                                adapter.reduceData { root ->
                                    root.updateRoot(item.update(isOpen = !item.isOpen))
                                }
                            }
                        },
                        recycleFun = { bind ->
                            bind.name = null
                            bind.isOpen = null
                            bind.leftAnchor.setLeftMarginDp(0)
                            bind.containerLayout.setOnClickListener(null)
                        })

        adapter = RendererAdapter.singleRenderer(sampleFiles,
                treeRenderer(folderItemRenderer, fileRenderer))
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
}

sealed class FolderNode {
    object Folder : FolderNode()
    object File : FolderNode()
}

val sampleFiles = TreeNodeBuilder.buildTree<FolderNode.Folder, FolderNode.File> {
    addLeaf("file1", FolderNode.File)

    addNode("program", FolderNode.Folder) {
        addLeaf("clear.sh", FolderNode.File)
        addLeaf("thumbnail.db", FolderNode.File)
        addNode("AndroidStudio", FolderNode.Folder) {
            addLeaf("studio32.sh", FolderNode.File)
            addLeaf("studio64.sh", FolderNode.File)
        }
        addLeaf("temp.db", FolderNode.File)
    }

    // Infinitely recursive lazy folder
    // 无限递归的惰性文件夹
    lateinit var subCreator: TreeBuilderFun<FolderNode.Folder, FolderNode.File>
    var times = 0
    subCreator = {
        addNodeLazy("Infinitely Folder${times++}", FolderNode.Folder, isOpen = false) {
            addLeaf("img1.jpg", FolderNode.File)
            addLeaf("img2.jpg", FolderNode.File)
            subCreator()
        }
    }
    subCreator()

    addLeaf("file2", FolderNode.File)
    addLeaf("file3", FolderNode.File)

    addNode("projects", FolderNode.Folder) {
        addNode("DslAdapter", FolderNode.Folder) {
            addLeaf("Readme.md", FolderNode.File)
        }
    }

    addNode("picture", FolderNode.Folder) {
        addLeaf("img1.jpg", FolderNode.File)
        addLeaf("img2.jpg", FolderNode.File)
        addLeaf("img3.jpg", FolderNode.File)
    }
}


fun Char.repeat(count: Int): String =
        (1..count).fold(StringBuilder()) { builder, _ -> builder.append(this@repeat) }.toString()

fun TreeNode<*, *>.getSpace(): String = ' '.repeat(deep * 5)







