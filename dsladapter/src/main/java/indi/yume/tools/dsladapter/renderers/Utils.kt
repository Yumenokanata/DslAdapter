package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.datatype.ActionComposite
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.Renderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import java.util.*
import kotlin.coroutines.experimental.buildSequence

typealias KeyGetter<T> = (T, Int) -> Any?

fun <T1, T2, T3, V> zip3(it1: Iterable<T1>, it2: Iterable<T2>, it3: Iterable<T3>,
                         zipper: (T1, T2, T3) -> V): Sequence<V> =
        buildSequence {
            val iter1 = it1.iterator()
            val iter2 = it2.iterator()
            val iter3 = it3.iterator()

            while (iter1.hasNext() && iter2.hasNext() && iter3.hasNext()) {
                yield(zipper(iter1.next(), iter2.next(), iter3.next()))
            }
        }

fun <VD: ViewData<*>> List<VD>.getEndsPonints(): IntArray =
        getEndsPonints { it.count }

fun IntArray.getEndPoint(defaultV: Int = 0): Int = lastOrNull() ?: defaultV

fun IntArray.getTargetStartPoint(index: Int): Int = when {
    index < 0 -> 0
    index > lastIndex -> get(lastIndex)
    else -> get(index - 1)
}

fun <T> List<T>.getEndsPonints(getter: (T) -> Int): IntArray {
    val ends = IntArray(size)
    var lastEndPosition = 0
    for ((i, vd) in withIndex()) {
        lastEndPosition += getter(vd)
        ends[i] = lastEndPosition
    }

    return ends
}

fun resolveIndices(position: Int, endPos: IntArray): Pair<Int, Int> {
    if (position < 0 || position >= endPos.getEndPoint()) {
        throw IndexOutOfBoundsException(
                "Asked for position $position while count is ${endPos.getEndPoint()}")
    }

    var arrayIndex = Arrays.binarySearch(endPos, position)
    if (arrayIndex >= 0) {
        do {
            arrayIndex++
        } while (endPos[arrayIndex] == position)
    } else {
        arrayIndex = arrayIndex.inv()
    }

    val resolvedRepositoryIndex = arrayIndex
    val resolvedItemIndex = if (arrayIndex == 0) position else position - endPos[arrayIndex - 1]

    return resolvedRepositoryIndex to resolvedItemIndex
}

fun List<UpdateActions>.filterUselessAction(): List<UpdateActions> =
        map {
            if (it is ActionComposite)
                if (it.actions.isEmpty())
                    null
                else {
                    val subActions = it.actions.filterUselessAction()
                    if (subActions.isEmpty())
                        null
                    else
                        ActionComposite(it.offset, subActions)
                }
            else
                it
        }.filterNotNull()
