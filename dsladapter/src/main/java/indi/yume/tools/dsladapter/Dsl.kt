package indi.yume.tools.dsladapter

import androidx.annotation.LayoutRes
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

fun <T: Any> layout(@LayoutRes layout: Int): LayoutRenderer<T> =
        LayoutRenderer(layout = layout)

fun <T> keyMe(): KeyGetter<T> = { i, _ -> i }


fun <T, VD: ViewData<T>, UP : Updatable<T, VD>> BaseRenderer<T, VD, UP>.forList(
        keyGetter: KeyGetter<T>? = null,
        demapper: (oldData: List<T>, newMapData: List<T>) -> List<T> = { oldData, newMapData -> newMapData })
        : ListRenderer<List<T>, T, VD, UP> =
        ListRenderer(converter = { it }, demapper = demapper, subs = this, keyGetter = keyGetter)

fun <T, VD: ViewData<T>, UP : Updatable<T, VD>> BaseRenderer<T, VD, UP>.forList(
        itemIsSingle: Boolean = false,
        keyGetter: KeyGetter<T>? = null,
        demapper: (oldData: List<T>, newMapData: List<T>) -> List<T> = { oldData, newMapData -> newMapData })
        : ListRenderer<List<T>, T, VD, UP> =
        ListRenderer(converter = { it }, demapper = demapper, subs = this, keyGetter = keyGetter, itemIsSingle = itemIsSingle)

fun <T, D, VD: ViewData<D>, UP : Updatable<D, VD>> BaseRenderer<D, VD, UP>.map(mapper: (T) -> D,
                                                                               demapper: (oldData: T, newMapData: D) -> T)
        : MapperRenderer<T, D, VD, UP> =
        MapperRenderer(mapper = mapper, demapper = demapper, targetRenderer = this)

fun <T, D, VD: ViewData<D>, UP : Updatable<D, VD>> BaseRenderer<D, VD, UP>.mapT(type: TypeCheck<T>,
                                                                                mapper: (T) -> D,
                                                                                demapper: (oldData: T, newMapData: D) -> T)
        : MapperRenderer<T, D, VD, UP> =
        MapperRenderer(mapper = mapper, demapper = demapper, targetRenderer = this)

fun <T, VD : ViewData<T>, UP : Updatable<T, VD>> BaseRenderer<T, VD, UP>.ignoreType(
        reduceFun: UP.(oldData: T, newData: T, payload: Any?) -> ActionU<VD>): IgnoreRenderer<T> =
        IgnoreRenderer.ignoreType(this, reduceFun)


fun <T, VD : ViewData<T>, UP : Updatable<T, VD>, TUP : Updatable<T, VD>>
        BaseRenderer<T, VD, UP>.replaceUpdate(reduceFun: (UP) -> TUP): ReplaceRenderer<T, VD, TUP> =
        ReplaceRenderer.replaceUpdate(this, reduceFun)
