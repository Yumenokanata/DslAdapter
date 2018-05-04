package indi.yume.tools.dsladapter3

import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.util.ArrayMap
import android.util.SparseArray
import indi.yume.tools.dsladapter3.datatype.ListUpdateActions
import indi.yume.tools.dsladapter3.datatype.toActions
import io.kotlintest.forAll
import io.kotlintest.properties.Gen
import io.kotlintest.properties.Gen.Companion.choose
import io.kotlintest.properties.Gen.Companion.list
import io.kotlintest.properties.Gen.Companion.string
import io.kotlintest.properties.forAll
import io.kotlintest.properties.map
import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.timerTask
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class DiffUtilTest {

    @Test
    fun diffUtilTest() {
        val oldData = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
//        val newData = listOf("4", "5", "6", "10", "7", "8", "9", "1", "2", "3")
        val newData = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
        val detectMoves = true

//        println("=======================")
//        println("old data: ${oldData.joinToString()}")
//        println("new data: ${newData.joinToString()}")
//        diffUtil(oldData, newData)

        forAll(list(choose(0, 9).map { it.toString() }), list(choose(0, 9).map { it.toString() }))
        { list1, list2 ->
            println("=======================")
            println("old data: ${list1.joinToString()}")
            println("new data: ${list2.joinToString()}")
            measureTimeMillis {
                diffUtil(list1, list2)
            }.apply { println("spend time: $this") }

            true
        }
    }

    fun diffUtil(oldData: List<String>, newData: List<String>, detectMoves: Boolean = false) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldData.size

            override fun getNewListSize(): Int = newData.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldData[oldItemPosition] == newData[newItemPosition]

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldData[oldItemPosition] == newData[newItemPosition]

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                println("getChangePayload: oldItemPosition=$oldItemPosition, newItemPosition=$newItemPosition")
                return super.getChangePayload(oldItemPosition, newItemPosition)
            }
        }, detectMoves)

        result.dispatchUpdatesTo(object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                println("onInserted: position=$position, count=$count")
            }

            override fun onRemoved(position: Int, count: Int) {
                println("onRemoved: position=$position, count=$count")
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                println("onMoved: fromPosition=$fromPosition, toPosition=$toPosition")
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                println("onChanged: position=$position, count=$count, payload=$payload")
            }
        })

        val realActions = result.toActions(oldData, newData)

        for(act in realActions)
            println("Real Act: $act")

        realActions.fold(oldData)
        { data, act ->
            val nData = when(act) {
                is ListUpdateActions.OnInserted -> data.subList(0, act.pos) + act.insertedItems + data.subList(act.pos, data.size)
                is ListUpdateActions.OnRemoved -> data.subList(0, act.pos) + data.subList(act.pos + act.count, data.size)
                is ListUpdateActions.OnMoved -> {
                    if (act.fromPosition < act.toPosition)
                        data.subList(0, act.fromPosition) + data.subList(act.fromPosition + 1, act.toPosition) + data[act.fromPosition] + data.subList(act.toPosition, data.size)
                    else
                        data.subList(0, act.toPosition) + data[act.fromPosition] + data.subList(act.toPosition, act.fromPosition) + data.subList(act.fromPosition + 1, data.size)
                }
                is ListUpdateActions.OnChanged -> data.subList(0, act.pos) + act.newItems + data.subList(act.pos + act.count, data.size)
            }
            println("act=$act | nData: $nData")
            nData
        }.apply { assert(this == newData, { joinToString() }) }.joinToString().let { println("result: $it") }

//        result.toActions().fold(oldData)
//        { data, act ->
//            val nData = when(act) {
//                is OnInserted -> data.subList(0, act.pos) + List(act.count, { "OnInserted-${act.hashCode()}" }) + data.subList(act.pos, data.size)
//                is OnRemoved -> data.subList(0, act.pos) + data.subList(act.pos + act.count, data.size)
//                is OnMoved -> {
//                    if (act.fromPosition < act.toPosition)
//                        data.subList(0, act.fromPosition) + data.subList(act.fromPosition + 1, act.toPosition) + data[act.fromPosition] + data.subList(act.toPosition, data.size)
//                    else
//                        data.subList(0, act.toPosition) + data[act.fromPosition] + data.subList(act.toPosition, act.fromPosition) + data.subList(act.fromPosition + 1, data.size)
//                }
//                is OnChanged -> data.subList(0, act.pos) + List(act.count, { "OnChanged-${act.hashCode()}" }) + data.subList(act.pos + act.count, data.size)
//            }
//
//            println("act=$act | nData: $nData")
//
//            nData
//        }.apply { assert(size == newData.size, { joinToString() }) }.joinToString().let { println("result: $it") }

//        result.toActions().reversed().fold(newData)
//        { data, act ->
//            when(act) {
//                is OnInserted -> data.subList(0, act.pos) + data.subList(act.pos + act.count, data.size)
//                is OnRemoved -> data.subList(0, act.pos) + List(act.count, { "OnRemoved ${act.hashCode()}" }) + data.subList(act.pos, data.size)
//                is OnMoved -> {
//                    if (act.fromPosition < act.toPosition)
//                        data.subList(0, act.fromPosition) + data[act.toPosition] + data.subList(act.fromPosition, act.toPosition) + data.subList(act.toPosition + 1, data.size)
//                    else
//                        data.subList(0, act.toPosition) + data.subList(act.toPosition + 1, act.fromPosition) + data[act.toPosition] + data.subList(act.fromPosition, data.size)
//                }
//                is OnChanged -> data.subList(0, act.pos) + List(act.count, { "OnChanged ${act.hashCode()}" }) + data.subList(act.pos + act.count, data.size)
//            }
//        }.joinToString().let { println("result: $it") }
    }
}