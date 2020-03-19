package indi.yume.tools.dsladapter.extensions

import arrow.core.Eval
import org.junit.Test

import org.junit.Assert.*

sealed class FolderNode {
    object Folder : FolderNode()
    object File : FolderNode()
}

class TreeUtilTest {
    val testSampleFiles = TreeNodeBuilder.buildTree<FolderNode.Folder, FolderNode.File> {
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

    @Test
    fun getParentPathString() {
    }

    @Test
    fun getRoot() {
        val studio32shNode = testSampleFiles.relativePathToByName(
                listOf("program", "AndroidStudio", "studio32.sh")
        )

        assert(studio32shNode!!.getRoot() == testSampleFiles)
    }

    @Test
    fun relativePathToByName() {
        val rootNode = testSampleFiles.relativePathToByName(
                listOf()
        ).apply { println(this) }
        val file2Node = testSampleFiles.relativePathToByName(
                listOf("file2")
        ).apply { println(this) }
        val studio32shNode = testSampleFiles.relativePathToByName(
                listOf("program", "AndroidStudio", "studio32.sh")
        ).apply { println(this) }
        val dslAdapterNode = testSampleFiles.relativePathToByName(
                listOf("projects", "DslAdapter")
        ).apply { println(this) }

        val readMeNode = dslAdapterNode?.relativePathToByName(
                listOf("Readme.md")
        ).apply { println(this) }

        val nullNode = testSampleFiles.relativePathToByName(
                listOf("projects", "DslAdapter", "null")
        ).apply { println(this) }

        assert(rootNode != null)
        assert(file2Node != null)
        assert(studio32shNode != null)
        assert(dslAdapterNode != null)
        assert(readMeNode != null)
        assert(nullNode == null)
    }

    @Test
    fun updateRoot() {
        val dslAdapterNode = testSampleFiles.relativePathToByName(
                listOf("projects", "DslAdapter")
        )!! as TreeNode.Node<FolderNode.Folder, FolderNode.File>

        println("old:\n${dslAdapterNode.prettyString()}")

        val newDslAdapterNode = dslAdapterNode.updateF(name = "new DslAdapter") {
            add(TreeBuilderNode.Leaf("new file", FolderNode.File))
            add(TreeBuilderNode.Node("new folder", FolderNode.Folder, Eval.just(listOf(
                    TreeBuilderNode.Leaf("file11", FolderNode.File),
                    TreeBuilderNode.Leaf("file22", FolderNode.File)
            ))))
        }

        println("new:\n${newDslAdapterNode.prettyString()}")

        val newSampleTree =
                dslAdapterNode.updateFromRoot(newDslAdapterNode)

        println("old Tree:\n${testSampleFiles.prettyString()}")
        println("new Tree:\n${newSampleTree.prettyString()}")
    }
}