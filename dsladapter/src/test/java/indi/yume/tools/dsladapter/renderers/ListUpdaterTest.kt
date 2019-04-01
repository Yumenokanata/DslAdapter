package indi.yume.tools.dsladapter.renderers

import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.forList
import org.junit.Test

import org.junit.Assert.*

class ListUpdaterTest {

    @Test
    fun updateAuto() {
        val itemRenderer = MockRenderer<Int> { it % 10 }
        val keyGetter = { i: Int, _: Int -> i % 100 }
        val listRenderer = itemRenderer.forList(keyGetter)
        val listData = (0..5).toList()

        val testData = listOf(
                UpdateData(listData, listOf(0, 1, 2, 3, 4, 5), ActionComposite(0, emptyList())),
                UpdateData(listData, emptyList(), ActionComposite(0, listOf(OnRemoved(0, 15)))),
                UpdateData(listData, listOf(0, 1, 2, 4, 5), ActionComposite(0, listOf(OnRemoved(3, 3)))),
                UpdateData(listData, listOf(0, 3, 1, 2, 4, 5), ActionComposite(0, listOf(OnRemoved(3, 3), OnInserted(0, 3)))),
                UpdateData(listData, listOf(1, 2, 3, 4, 5), ActionComposite(0, listOf(OnRemoved(0, 0)))),
                UpdateData(listData, listOf(0, 1, 2, 3, 4), ActionComposite(0, listOf(OnRemoved(10, 5)))),
                UpdateData(listData, listOf(0, 1, 2, 13, 4, 5), ActionComposite(0, listOf(OnRemoved(3, 3), OnInserted(3, 3)))),
                UpdateData(listOf(0, 1, 2, 13, 4, 5), listOf(0, 1, 2, 113, 4, 5), ActionComposite(0, listOf(OnChanged(3, 3, null)))),
                UpdateData(listOf(11, 1, 2, 3, 4, 5), listOf(3, 1, 13, 2, 11, 4, 5), ActionComposite(0, listOf(
                        OnInserted(pos=2, count=3), OnMoved(fromPosition=0, toPosition=7),
                        OnRemoved(pos=7, count=3), OnInserted(pos=0, count=3)))),
                UpdateData(listOf(0, 1, 2, 3, 4, 5), listOf(3, 1, 13, 2, 0, 4, 5), ActionComposite(0, listOf(
                        OnInserted(pos=1, count=3),
                        OnInserted(pos=6, count=0), OnRemoved(pos=0, count=0),
                        OnRemoved(pos=6, count=3), OnInserted(pos=0, count=3)))),
                UpdateData(emptyList(), listOf(0, 1, 2, 3, 4, 5), ActionComposite(0, listOf(OnInserted(0, 15))))
        )

        for ((oldData, newData, result) in testData) {
            val oldViewData = ListViewData(oldData, oldData, oldData.map { itemRenderer.getData(it) })
//            val value: UpdateActions = listRenderer.updater.updateDiffUtil(newData, keyGetter)(oldViewData).first
//            assert(result == value) { "oldData=${oldData.joinToString()}\n newData=${newData.joinToString()}\n error result=$value;\n right result=$result" }
        }
    }

    data class UpdateData<T>(val oldData: List<T>, val newData: List<T>, val result: UpdateActions)
}