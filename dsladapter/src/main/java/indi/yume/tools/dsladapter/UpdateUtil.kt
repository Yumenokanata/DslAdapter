package indi.yume.tools.dsladapter

import arrow.Kind
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.datatype.UpdateActions
import indi.yume.tools.dsladapter.typeclass.ViewData


typealias ActionU<VD> = (VD) -> Pair<UpdateActions, VD>


class TypeCheck<T>

private val typeFake = TypeCheck<Nothing>()

@Suppress("UNCHECKED_CAST")
fun <T> type(): TypeCheck<T> = typeFake as TypeCheck<T>


data class IdT<T>(val a: T) : IdTOf<T>

class ForIdT private constructor() { companion object }
typealias IdTOf<T> = Kind<ForIdT, T>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> IdTOf<T>.fix(): IdT<T> =
        this as IdT<T>
@Suppress("NOTHING_TO_INLINE")
inline fun <T> IdTOf<T>.value(): T = fix().a

fun <T> T.toIdT(): IdT<T> = IdT(this)

fun <E> HListK.Companion.singleId(e: E): HConsK<ForIdT, E, HNilK<ForIdT>> = HConsK(IdT(e), nil())

// put item to First
fun <E, L : HListK<ForIdT, L>> L.putF(e: E): HConsK<ForIdT, E, L> = extend(IdT(e))



data class ChangedData<T>(val newData: T, val payload: Any? = null)

fun <T> changed(newData: T, payload: Any? = null): ChangedData<T> = ChangedData(newData, payload)


fun <P, VD : ViewData<P>> updateVD(oldVD: VD, newVD: VD, payload: Any? = null): UpdateActions = when {
        newVD == oldVD -> EmptyAction
        newVD.count == oldVD.count -> OnChanged(0, newVD.count, payload)
        else -> ActionComposite(0, listOf(
                OnRemoved(0, oldVD.count),
                OnInserted(0, newVD.count)
        ))
    }

operator fun <T, VD : ViewData<T>> ActionU<VD>.plus(a2: ActionU<VD>): ActionU<VD> =
        { oldVD ->
            val (firstAct, firstVD) = this@plus(oldVD)
            val (secondAct, secondVD) = a2(firstVD)
            ActionComposite(0, listOf(firstAct, secondAct)) to secondVD
        }



fun <T> List<T>.move(from: Int, to: Int): List<T>? {
    if (from !in 0 until size || to !in 0 until size || from == to)
        return null

    val result = toMutableList()
    val target = get(from)

    result.removeAt(from)
    result.add(to, target)

    return result.toList()
}