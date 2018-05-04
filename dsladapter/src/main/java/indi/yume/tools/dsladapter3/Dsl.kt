package indi.yume.tools.dsladapter3

import android.support.annotation.LayoutRes
import indi.yume.tools.dsladapter3.renderers.LayoutRenderer
import indi.yume.tools.dsladapter3.renderers.ListRenderer
import indi.yume.tools.dsladapter3.typeclass.BaseRenderer
import indi.yume.tools.dsladapter3.typeclass.Renderer
import indi.yume.tools.dsladapter3.typeclass.ViewData

fun <T: Any> layout(@LayoutRes layout: Int): LayoutRenderer<T> =
        LayoutRenderer(layout = layout)

fun <T, VD: ViewData> BaseRenderer<T, VD>.forList(keyGetter: (VD, Int) -> Any? = { i, index -> i }): ListRenderer<List<T>, T, VD> =
        ListRenderer(converter = { it }, subs = this, keyGetter = keyGetter)
