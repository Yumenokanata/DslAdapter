package indi.yume.tools.dsladapter.updater

import androidx.annotation.CheckResult
import arrow.Kind
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.UpdateResult
import indi.yume.tools.dsladapter.datatype.ActionComposite
import indi.yume.tools.dsladapter.renderers.IgnoreRenderer
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


interface Updatable<P, VD : ViewData<P>> : UpdatableOf<P, VD>

class ForUpdatable private constructor() { companion object }
typealias UpdatableOf<P, VD> = Kind<ForUpdatable, Pair<P, VD>>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <P, VD> UpdatableOf<P, VD>.fix(): Updatable<P, VD> where VD : ViewData<P> =
        this as Updatable<P, VD>


operator fun <T, VD : ViewData<T>> ActionU<VD>.plus(a2: ActionU<VD>): ActionU<VD> =
        { oldVD ->
            val (firstAct, firstVD) = this@plus(oldVD)
            val (secondAct, secondVD) = a2(firstVD)
            ActionComposite(0, listOf(firstAct, secondAct)) to secondVD
        }


fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>, UP : Updatable<T, VD>> BR.ignoreTypeU(
        updatable: (BR) -> UP,
        reduceFun: UP.(oldData: T, newData: T, payload: Any?) -> ActionU<VD>): IgnoreRenderer<T> =
        IgnoreRenderer.ignoreType(this) { p1, p2, p3 -> updatable(this).reduceFun(p1, p2, p3) }


@CheckResult
fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>, UP : Updatable<T, VD>> RendererAdapter<T, VD, BR>
        .reduce(updatable: (BR) -> UP, f: UP.(T) -> ActionU<VD>): UpdateResult<T, VD> {
    val data = getViewData()
    val (actions, newVD) = updatable(renderer).f(data.originData)(data)

    return UpdateResult(data, newVD, listOf(actions))
}

@CheckResult
fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>, UP : Updatable<T, VD>> RendererAdapter<T, VD, BR>
        .update(updatable: (BR) -> UP, f: UP.() -> ActionU<VD>): UpdateResult<T, VD> {
    val data = getViewData()
    val (actions, newVD) = updatable(renderer).f()(data)

    return UpdateResult(data, newVD, listOf(actions))
}

fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>, UP : Updatable<T, VD>> RendererAdapter<T, VD, BR>
        .updateNow(updatable: (BR) -> UP, f: UP.() -> ActionU<VD>) {
    update(updatable, f).dispatchUpdatesTo(this)
}
