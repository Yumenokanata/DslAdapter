package indi.yume.tools.dsladapter

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_forColor() {
        val background = "#f4c80000".parseColor()
        val foreground = "#ccffffff".parseColor()
        val result = "#f4f4cccc".parseColor()

        assertEquals(background + foreground, result)
        assertEquals(result.minusForeground(foreground), background)
        assertEquals(result.minusBackground(background, foreground.alpha), foreground)

        println("#ffcccccc".parseColor().minusBackground("#ff000000".parseColor(), 204).toColorString())
    }

}

fun String.parseColor(): Color =
        Color(alpha = substring(1, 3).toShort(16),
                red = substring(3, 5).toShort(16),
                green = substring(5, 7).toShort(16),
                blue = substring(7, 9).toShort(16))

/**
 * background + foreground = result
 */
operator fun Color.plus(c2: Color): Color =
        Color(alpha = alpha,
                red = plusColor(c2.alpha, red, c2.red),
                green = plusColor(c2.alpha, green, c2.green),
                blue = plusColor(c2.alpha, blue, c2.blue))

/**
 * result - foreground = background
 */
fun Color.minusForeground(c2: Color): Color =
        Color(alpha = alpha,
                red = minusForeColor(c2.alpha, red, c2.red),
                green = minusForeColor(c2.alpha, green, c2.green),
                blue = minusForeColor(c2.alpha, blue, c2.blue))

/**
 * result - background = foreground
 */
fun Color.minusBackground(c2: Color, foregroundAlpha: Short): Color =
        Color(alpha = foregroundAlpha,
                red = minusBackColor(foregroundAlpha, red, c2.red),
                green = minusBackColor(foregroundAlpha, green, c2.green),
                blue = minusBackColor(foregroundAlpha, blue, c2.blue))

private fun plusColor(foregroundAlpha: Short, backgroundColor: Short, foregroundColor: Short): Short =
        (backgroundColor + (foregroundColor - backgroundColor) * (foregroundAlpha / 255f)).toShort()

/**
 * @return background color
 */
private fun minusForeColor(resultAlpha: Short, resultColor: Short, foregroundColor: Short): Short =
        ((255 * resultColor - resultAlpha * foregroundColor) * 1f / (255 - resultAlpha)).toShort()

/**
 * @return foreground color
 */
private fun minusBackColor(resultAlpha: Short, resultColor: Short, backgroundColor: Short): Short =
        (255f * (resultColor - backgroundColor) / resultAlpha + backgroundColor).toShort()

data class Color(val alpha: Short, val red: Short, val green: Short, val blue: Short) {
    fun toColorString(): String =
            "#${alpha.toString(16)}${red.toString(16)}${green.toString(16)}${blue.toString(16)}"
}
