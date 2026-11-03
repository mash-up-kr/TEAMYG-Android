package com.teamyg.parfait.feature.groups.canvas.impl.component

import android.content.res.Configuration
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 오토핏 측정이 시스템 글자 크기(`fontScale`)를 따라가면 렌더링과 어긋나 글자가 과하게 줄어든다.
 * 같은 dp 예산이면 fontScale 1.0과 1.15의 결과가 같아야 한다.
 */
@SmallTest
class TextToppingAutoFitTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun fitTextToppingToHeight_fontScale115_matchesFontScaleOne() {
        val base = fit(fontScale = 1f)
        val scaled = fit(fontScale = 1.15f)

        assertTrue(base.isOverflowing)
        assertEquals(base.isOverflowing, scaled.isOverflowing)
        assertEquals(base.fontSizeSp, scaled.fontSizeSp, 0.05f)
        assertEquals(base.fieldMaxWidthDp.value, scaled.fieldMaxWidthDp.value, 0.05f)
    }

    private fun fit(fontScale: Float): TextToppingAutoFit {
        val configuration = Configuration(context.resources.configuration).apply { this.fontScale = fontScale }
        val density = Density(context.createConfigurationContext(configuration))
        val textMeasurer = TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(context),
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
        return fitTextToppingToHeight(
            textMeasurer = textMeasurer,
            text = textToppingAnnotatedString("가나다라마바사\n아자차카\n타파하"),
            baseStyle = TextStyle(),
            maxWidthPx = with(density) { MAX_WIDTH.roundToPx() },
            maxWidthDp = MAX_WIDTH,
            maxHeightPx = with(density) { MAX_HEIGHT.roundToPx() },
            maxFontSizeSp = 24f,
            density = density,
        )
    }

    private companion object {
        val MAX_WIDTH = 200.dp
        val MAX_HEIGHT = 60.dp
    }
}
