package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.text.AnnotatedString
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextToppingScriptSpansTest {
    @Test
    fun annotatedString_onePerRunSpanAndTextUnchanged() {
        val result = textToppingAnnotatedString("パフェ 파르페")
        assertEquals("パフェ 파르페", result.text)
        assertEquals(
            listOf(
                AnnotatedString.Range(TextToppingScript.JAPANESE.spanStyle, 0, 4),
                AnnotatedString.Range(TextToppingScript.DEFAULT.spanStyle, 4, 7),
            ),
            result.spanStyles,
        )
    }

    @Test
    fun annotatedString_empty_noSpans() {
        assertTrue(textToppingAnnotatedString("").spanStyles.isEmpty())
    }
}
