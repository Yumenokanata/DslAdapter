package indi.yume.tools.dsladapter

import androidx.recyclerview.widget.ListUpdateCallback
import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.some
import indi.yume.tools.dsladapter.datatype.*
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.Renderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import org.junit.Test

class UpdatesTest {

    @Test
    fun testUpdate() {
        val oldData: List<Pair<String, List<String>>> =
                listOf(
                        "sub1" to listOf(),
                        "sub2" to listOf(),
                        "sub3" to listOf(),
                        "sub4" to listOf()
                )
        val newData: List<Pair<String, List<String>>> =
                listOf(
                        "sub1" to listOf("Sub 11", "Sub 22", "Sub 13"),
                        "sub2" to listOf("Sub 21", "Sub 22", "Sub 23"),
                        "sub3" to listOf("Sub 31", "Sub 32", "Sub 33"),
                        "sub4" to listOf("Sub 41", "Sub 42", "Sub 43")
                )

        val renderer = optionRenderer(
                noneItemRenderer = ConstantItemRenderer(type = type<Unit>(), count = 3, layout = 1, data = Unit),
                itemRenderer = TitleItemRenderer(
                        itemType = type<Pair<String, List<String>>>(),
                        titleGetter = { it.first },
                        subsGetter = { it.second },
                        title = LayoutRenderer(count = 3, layout = 3),
                        subs = LayoutRenderer(count = 2, layout = 4)
                ).forList()
        )
    }

    data class MoveTestModel<T>(val ori: List<T>, val from: Int, val to: Int, val target: List<T>?)

    @Test
    fun testMove() {
        val sample = listOf(
                MoveTestModel(listOf(1, 2, 3, 4), 0, 3, listOf(2, 3, 4, 1)),
                MoveTestModel(listOf(1, 2, 3, 4), 3, 0, listOf(4, 1, 2, 3)),
                MoveTestModel(listOf(1, 2, 3, 4), 1, 2, listOf(1, 3, 2, 4)),
                MoveTestModel(listOf(1, 2, 3, 4), 2, 1, listOf(1, 3, 2, 4)),
                MoveTestModel(listOf(1, 2, 3, 4), 0, 0, null),
                MoveTestModel(listOf(1, 2, 3, 4), 3, 3, null),
                MoveTestModel(listOf(1, 2, 3, 4), -1, 3, null),
                MoveTestModel(listOf(1, 2, 3, 4), 4, 3, null),
                MoveTestModel(listOf(1, 2, 3, 4), 0, 4, null),
                MoveTestModel(listOf(1, 2, 3, 4), 0, Int.MAX_VALUE, null)
        )

        for (t in sample)
            assert(t.ori.move(t.from, t.to) == t.target) { t.toString() + " result: ${t.ori.move(t.from, t.to)}" }
    }
}


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