package indi.yume.tools.dsladapter

import androidx.annotation.LayoutRes
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

fun <T: Any> layout(@LayoutRes layout: Int): LayoutRenderer<T> =
        LayoutRenderer(layout = layout)

fun <T> keyMe(): KeyGetter<T> = { i, _ -> i }


fun <T, VD: ViewData<T>> BaseRenderer<T, VD>.forList(
        keyGetter: KeyGetter<T>? = null,
        demapper: (oldData: List<T>, newMapData: List<T>) -> List<T> = { oldData, newMapData -> newMapData })
        : ListRenderer<List<T>, T, VD> =
        ListRenderer(converter = { it }, demapper = demapper, subs = this, keyGetter = keyGetter)

fun <T, VD: ViewData<T>> BaseRenderer<T, VD>.forList(
        itemIsSingle: Boolean = false,
        keyGetter: KeyGetter<T>? = null,
        demapper: (oldData: List<T>, newMapData: List<T>) -> List<T> = { oldData, newMapData -> newMapData })
        : ListRenderer<List<T>, T, VD> =
        ListRenderer(converter = { it }, demapper = demapper, subs = this, keyGetter = keyGetter, itemIsSingle = itemIsSingle)

fun <T, D, VD: ViewData<D>> BaseRenderer<D, VD>.map(mapper: (T) -> D,
                                                             demapper: (oldData: T, newMapData: D) -> T)
        : MapperRenderer<T, D, VD> =
        MapperRenderer(mapper = mapper, demapper = demapper, targetRenderer = this)

fun <T, D, VD : ViewData<D>> BaseRenderer<D, VD>.mapT(type: TypeCheck<T>,
                                                               mapper: (T) -> D,
                                                               demapper: (oldData: T, newMapData: D) -> T)
        : MapperRenderer<T, D, VD> =
        MapperRenderer(mapper = mapper, demapper = demapper, targetRenderer = this)

fun <T, VD : ViewData<T>> BaseRenderer<T, VD>.ignoreType(
        reduceFun: BaseRenderer<T, VD>.(oldData: T, newData: T, payload: Any?) -> ActionU<VD> =
                { oldData, newData, payload -> defaultUpdater.autoUpdate(newData) }): IgnoreRenderer<T> =
        IgnoreRenderer.ignoreType(this, reduceFun)

