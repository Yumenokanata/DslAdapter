package indi.yume.tools.dsladapter


internal fun Char.repeat(count: Int): String =
        (1..count).fold(StringBuilder()) { builder, _ -> builder.append(this@repeat) }.toString()