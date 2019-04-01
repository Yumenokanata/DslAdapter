package indi.yume.tools.dsladapter.renderers

import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import org.junit.Test

import org.junit.Assert.*

class UtilsKtTest {

    @Test
    fun getTargetStartPoint() {
        forAll(Gen.oneOf(Gen.list(Gen.int()).map { it.toIntArray() }, Gen.create { intArrayOf() }))
        { intArray ->
            val size = intArray.size
            for (i in -3 until size + 3) {
                val result = intArray.getTargetStartPoint(i)
                val value = when {
                    size == 0 -> 0
                    i <= 0 -> 0
                    i >= size -> intArray[size - 1]
                    else -> intArray[i - 1]
                }

                assert(result == value) { "intArray=${intArray.joinToString()}; index=$i; result=$result; value=$value" }
            }
            true
        }
    }
}