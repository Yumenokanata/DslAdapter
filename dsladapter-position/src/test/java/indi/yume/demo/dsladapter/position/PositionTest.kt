package indi.yume.demo.dsladapter.position

import arrow.core.Either
import arrow.core.right
import indi.yume.tools.dsladapter.*
import indi.yume.tools.dsladapter.datatype.get0
import indi.yume.tools.dsladapter.datatype.get1
import indi.yume.tools.dsladapter.datatype.hlistKOf
import indi.yume.tools.dsladapter.renderers.*
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class PositionTest {
    @Test
    fun testPosGet() {
        val adapter = RendererAdapter.multipleBuild()
                .add(Unit, layout<Unit>(3))
                .add(listOf(Unit), layout<Unit>(3).forList())
                .build()

        val f = adapter.renderer.pos {
            itemPos({ get1() })
        }

        val f2 = adapter.renderer.pos {
            itemPos({ get0() }) {
                subsPos({ list -> list.size - 1 })
            }
        }

        f2(adapter.getViewData())
                .fold({ println("error: $it") },
                        { pos -> println("pos: $pos") })
    }

    @Test
    fun testLayoutRendererPos() {
        val adapter = RendererAdapter.singleRenderer("test", LayoutRenderer(MOCK_LAYOUT_RES, 3))

        val pos = adapter.renderer.pos { me() }(adapter.getViewData())
        assert(pos is Either.Right<RendererPos> && pos.b == RendererPos(0, 3))
        { "LayoutRenderer get position has error: result pos=$pos, not is RendererPos(start=0, count=3)" }
    }

    @Test
    fun testEmptyRendererPos() {
        val adapter = RendererAdapter.singleRenderer("test", EmptyRenderer())

        val pos = adapter.renderer.pos { me() }(adapter.getViewData())
        val rightResult = RendererPos(0, 0).right()

        assert(pos == rightResult)
        { "EmptyRenderer get position has error: result pos=$pos, not is $rightResult" }
    }

    @Test
    fun testConstantRendererPos() {
        val adapter = RendererAdapter.singleRenderer(TestModel(1, "test"),
                ConstantItemRenderer(type = type<TestModel>(), count = 3, layout = MOCK_LAYOUT_RES, data = "constant"))

        val pos = adapter.renderer.pos { me() }(adapter.getViewData())
        val rightResult = RendererPos(0, 3).right()

        assert(pos == rightResult)
        { "ConstantItemRenderer get position has error: result pos=$pos, not is $rightResult" }
    }

    @Test
    fun testIgnoreRendererPos() {
        val adapter = RendererAdapter.singleRenderer("test",
                LayoutRenderer<String>(MOCK_LAYOUT_RES, 3)
                        .ignoreType { oldData, newData, payload -> {
                            updateVD(getData(oldData), getData(newData), payload) to getData(newData)
                        } })

        val pos = adapter.renderer.pos { me() }(adapter.getViewData())
        val rightResult = RendererPos(0, 3).right()

        assert(pos == rightResult)
        { "IgnoreRenderer get position has error: result pos=$pos, not is $rightResult" }
    }

    @Test
    fun testMapperRendererPos() {
        val adapter = RendererAdapter.singleRenderer(TestModel(1, "test"),
                LayoutRenderer<String>(MOCK_LAYOUT_RES, 3)
                        .mapT(type = type<TestModel>(),
                                mapper = { it.msg },
                                demapper = { oldData, newMapData -> oldData.copy(msg = newMapData) }))

        val pos = adapter.renderer.pos { me() }(adapter.getViewData())
        val rightResult = RendererPos(0, 3).right()

        assert(pos == rightResult)
        { "MapperRenderer get position has error: result pos=$pos, not is $rightResult" }
    }

    @Test
    fun testListRendererPos() {
        val adapter = RendererAdapter.singleRenderer(getTestData(),
                LayoutRenderer<TestModel>(MOCK_LAYOUT_RES, 2)
                        .forList())

        val pos = adapter.renderer.pos {
            subsPos(10)
        }(adapter.getViewData())
        val rightResult = RendererPos(20, 2).right()

        assert(pos == rightResult)
        { "ListRenderer get position has error: result pos=$pos, not is $rightResult" }

        val pos2 = adapter.renderer.pos {
            subsPos({ list -> list.indexOfFirst { it.id == 10 } })
        }(adapter.getViewData())
        val rightResult2 = RendererPos(20, 2).right()

        assert(pos2 == rightResult2)
        { "ListRenderer get position has error: result pos2=$pos2, not is $rightResult2" }
    }

    @Test
    fun testSealedRendererPos() {
        val adapter = RendererAdapter.singleRenderer(getTestData(),
                SealedItemRenderer(hlistKOf(
                        item(type = type<TestModel>(),
                                checker = { it.id % 2 == 1 },
                                mapper = { it },
                                demapper = { oldData, newMapData -> newMapData },
                                renderer = LayoutRenderer<TestModel>(MOCK_LAYOUT_RES, 2)),
                        item(type = type<TestModel>(),
                                checker = { true },
                                mapper = { it },
                                demapper = { oldData, newMapData -> newMapData },
                                renderer = ConstantItemRenderer(type = type<TestModel>(),
                                        count = 3, layout = MOCK_LAYOUT_RES, data = "constant"))
                )).forList())

        val pos = adapter.renderer.pos {
            subsPos(10) {
                sealedItem({ this.head })
            }
        }(adapter.getViewData())
        val rightResult = RendererPos(25, 3).right()

        assert(pos == rightResult)
        { "SealedItemRenderer get position has error: result pos=$pos, not is $rightResult" }


        val pos1 = adapter.renderer.pos {
            subsPos(9) {
                sealedItemWithVD({
                    if (it.id % 2 == 1) get1() else get0()
                })
            }
        }(adapter.getViewData())
        val rightResult1 = RendererPos(23, 2).right()

        assert(pos1 == rightResult1)
        { "SealedItemRenderer get position has error: result pos1=$pos1, not is $rightResult1" }


        val pos2 = adapter.renderer.pos {
            subsPos(9) {
                sealedItem({ this.head })
            }
        }(adapter.getViewData())
        println("debug: SealedItemRenderer pos2=${pos2.fold({ it.formatMsg }, { it.toString() })}")

        assert(pos2.isLeft())
        { "SealedItemRenderer get position has error: result pos2=$pos2, not is Left" }
    }

    @Test
    fun testComposeRendererPos() {
        val adapter = RendererAdapter.multipleBuild()
                .add(TestModel(2, "test"),
                        ConstantItemRenderer(type = type<TestModel>(),
                                count = 3, layout = MOCK_LAYOUT_RES, data = "constant"))
                .add(getTestData(3), LayoutRenderer<TestModel>(MOCK_LAYOUT_RES, 2)
                        .forList())
                .add(TestModel(2, "test"),
                        ConstantItemRenderer(type = type<TestModel>(),
                                count = 3, layout = MOCK_LAYOUT_RES, data = "constant"))
                .build()

        val pos = adapter.renderer.pos {
            itemPos({ get0() })
        }(adapter.getViewData())
        val rightResult = RendererPos(9, 3).right()

        assert(pos == rightResult)
        { "ComposeRenderer get position has error: result pos=$pos, not is $rightResult" }


        val pos2 = adapter.renderer.pos {
            itemPos({ get1() }) {
                subsPos(2)
            }
        }(adapter.getViewData())
        val rightResult2 = RendererPos(7, 2).right()

        assert(pos2 == rightResult2)
        { "ComposeRenderer get position has error: result pos2=$pos2, not is $rightResult2" }
    }

    @Test
    fun testTitleRendererPos() {
        val adapter = RendererAdapter.singleRenderer("testTitle" to getTestData(),
                TitleItemRenderer(itemType = type<Pair<String, List<TestModel>>>(),
                        titleGetter = { pair -> pair.first },
                        subsGetter = { pair -> pair.second },
                        title = ConstantItemRenderer(type = type<String>(),
                                count = 3, layout = MOCK_LAYOUT_RES, data = "constant"),
                        subs = LayoutRenderer<TestModel>(MOCK_LAYOUT_RES, 2)))

        val pos = adapter.renderer.pos {
            titlePos()
        }(adapter.getViewData())
        val rightResult = RendererPos(0, 3).right()

        assert(pos == rightResult)
        { "TitleItemRenderer get position has error: result pos=$pos, not is $rightResult" }


        val pos2 = adapter.renderer.pos {
            itemsPos(10)
        }(adapter.getViewData())
        val rightResult2 = RendererPos(23, 2).right()

        assert(pos2 == rightResult2)
        { "TitleItemRenderer get position has error: result pos2=$pos2, not is $rightResult2" }
    }

    fun getTestData(count: Int = 200): List<TestModel> = (0 until count).map { TestModel(it, "Message: $it") }

    data class TestModel(val id: Int, val msg: String)

    val MOCK_LAYOUT_RES = 1
}