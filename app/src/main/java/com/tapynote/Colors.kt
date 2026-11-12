package com.tapynote

import android.graphics.drawable.GradientDrawable

object NoteColors {
    val fills = intArrayOf(
        0xFFFDF6D8.toInt(), // yellow
        0xFFFFE0E0.toInt(), // pink
        0xFFDDF2E3.toInt(), // green
        0xFFDDEBFA.toInt(), // blue
        0xFFEDE3F7.toInt(), // purple
        0xFFFFFFFF.toInt(), // white
        0xFF2B2B2B.toInt(), // dark
    )
    val widgetBg = intArrayOf(
        R.drawable.bg_0, R.drawable.bg_1, R.drawable.bg_2, R.drawable.bg_3,
        R.drawable.bg_4, R.drawable.bg_5, R.drawable.bg_6,
    )

    fun fill(i: Int) = fills[i.coerceIn(fills.indices)]
    fun text(i: Int) = if (i == 6) 0xFFF2F2F2.toInt() else 0xFF222222.toInt()
    fun hint(i: Int) = if (i == 6) 0x99FFFFFF.toInt() else 0x99000000.toInt()

    fun rounded(i: Int, radiusPx: Float) = GradientDrawable().apply {
        setColor(fill(i)); cornerRadius = radiusPx
    }
}
