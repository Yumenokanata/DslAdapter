package indi.yume.tools.dsladapter

import androidx.annotation.LayoutRes
import indi.yume.tools.dsladapter.renderers.KeyGetter
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
import indi.yume.tools.dsladapter.renderers.ListRenderer
import indi.yume.tools.dsladapter.renderers.MapperRenderer
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

fun <T: Any> layout(@LayoutRes layout: Int): LayoutRenderer<T> =
        LayoutRenderer(layout = layout)

fun <T, VD: ViewData<T>, UP : Updatable<T, VD>> BaseRenderer<T, VD, UP>.forList(demapper: (oldData: List<T>, newMapData: List<T>) -> List<T> =
                                                                                        { oldData, newMapData -> newMapData })
        : ListRenderer<List<T>, T, VD, UP> =
        ListRenderer(converter = { it }, demapper = demapper, subs = this)

fun <T, D, VD: ViewData<D>, UP : Updatable<D, VD>> BaseRenderer<D, VD, UP>.map(mapper: (T) -> D,
                                                                               demapper: (oldData: T, newMapData: D) -> T)
        : MapperRenderer<T, D, VD, UP> =
        MapperRenderer(mapper = mapper, demapper = demapper, targetRenderer = this)
