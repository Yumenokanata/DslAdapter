package indi.yume.tools.sample

import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import indi.yume.tools.dsladapter.Updatable
import indi.yume.tools.dsladapter.datatype.HConsK
import indi.yume.tools.dsladapter.datatype.HNilK
import indi.yume.tools.dsladapter.datatype.hlistKOf
import indi.yume.tools.dsladapter.renderers.ForSealedItem
import indi.yume.tools.dsladapter.renderers.SealedItemRenderer
import indi.yume.tools.dsladapter.renderers.item
import indi.yume.tools.dsladapter.type
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


fun <T, NVD : ViewData<Unit>, NUP : Updatable<Unit, NVD>,
        SVD : ViewData<T>, SUP : Updatable<T, SVD>> optionRenderer(noneItemRenderer: BaseRenderer<Unit, NVD, NUP>,
                                                                   itemRenderer: BaseRenderer<T, SVD, SUP>)
        : SealedItemRenderer<Option<T>, HConsK<Kind<ForSealedItem, Option<T>>, Pair<T, SUP>, HConsK<Kind<ForSealedItem, Option<T>>, Pair<Unit, NUP>, HNilK<Kind<ForSealedItem, Option<T>>>>>> =
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