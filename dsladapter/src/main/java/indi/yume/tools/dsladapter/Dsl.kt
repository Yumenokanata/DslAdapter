package indi.yume.tools.dsladapter

import androidx.annotation.LayoutRes
import indi.yume.tools.dsladapter.renderers.KeyGetter
import indi.yume.tools.dsladapter.renderers.LayoutRenderer
import indi.yume.tools.dsladapter.renderers.ListRenderer
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData

fun <T: Any> layout(@LayoutRes layout: Int): LayoutRenderer<T> =
        LayoutRenderer(layout = layout)

fun <T, VD: ViewData> BaseRenderer<T, VD>.forList(keyGetter: KeyGetter<VD> = { i, index -> i }): ListRenderer<List<T>, T, VD> =
        ListRenderer(converter = { it }, subs = this, keyGetter = keyGetter)
