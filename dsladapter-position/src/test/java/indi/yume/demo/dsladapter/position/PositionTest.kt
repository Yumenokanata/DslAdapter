package indi.yume.demo.dsladapter.position

import indi.yume.tools.dsladapter.RendererAdapter
import indi.yume.tools.dsladapter.datatype.get0
import indi.yume.tools.dsladapter.datatype.get1
import indi.yume.tools.dsladapter.forList
import indi.yume.tools.dsladapter.layout
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class PositionTest {
    @Test
    fun posGetTest() {
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
                        { pos -> pos.start })
    }
}