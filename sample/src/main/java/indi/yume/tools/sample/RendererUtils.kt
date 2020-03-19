package indi.yume.tools.sample

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Dimension
import androidx.annotation.Px
import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import indi.yume.tools.dsladapter.datatype.HConsK
import indi.yume.tools.dsladapter.datatype.HNilK
import indi.yume.tools.dsladapter.datatype.hlistKOf
import indi.yume.tools.dsladapter.renderers.ForSealedItem
import indi.yume.tools.dsladapter.renderers.SealedItemRenderer
import indi.yume.tools.dsladapter.renderers.item
import indi.yume.tools.dsladapter.type
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


fun <T, NVD : ViewData<Unit>,
        SVD : ViewData<T>> optionRenderer(noneItemRenderer: BaseRenderer<Unit, NVD>,
                                                                   itemRenderer: BaseRenderer<T, SVD>)
        : SealedItemRenderer<Option<T>, HConsK<Kind<ForSealedItem, Option<T>>, Pair<T, SVD>, HConsK<Kind<ForSealedItem, Option<T>>, Pair<Unit, NVD>, HNilK<Kind<ForSealedItem, Option<T>>>>>> =
        SealedItemRenderer(hlistKOf(
                item(type = type<Option<T>>(),
                        checker = { it is None },
                        mapper = { Unit },
                        renderer = noneItemRenderer
                ),
                item(type = type<Option<T>>(),
                        checker = { it is Some },
                        mapper = { it.orNull()!! },
                        renderer = itemRenderer
                )
        ))

fun Context.dp2px(dipValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (dipValue * scale + 0.5f).toInt()
}


fun View.setLeftMarginDp(@Dimension(unit = Dimension.DP) leftMargin: Int) {
    setLeftMargin(context.dp2px(leftMargin.toFloat()))
}

fun View.setLeftMargin(@Px leftMargin: Int) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.leftMargin = leftMargin
    layoutParams = params
}