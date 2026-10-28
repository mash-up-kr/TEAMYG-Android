package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.teamyg.parfait.core.designsystem.theme.typography.YGFontFamily
import com.teamyg.parfait.feature.groups.canvas.impl.R

private val lineSeedJpFontFamily: FontFamily = FontFamily(
    Font(R.font.line_seed_jp_regular, FontWeight.Normal),
)

private val lineSeedTwFontFamily: FontFamily = FontFamily(
    Font(R.font.line_seed_tw_regular, FontWeight.Normal),
)

/**
 * 텍스트 토핑의 글꼴·자간을 글자 구간마다 고른다 — 분할 규칙은 `util/TextToppingScriptRuns.kt`.
 *
 * - [JAPANESE]: 가나, 그리고 한자 우선순위에서 이 폰트가 먼저 글리프를 가진 한자 — Line Seed JP, 자간 -1.1%
 * - [CHINESE]: 한자 우선순위에서 이 폰트가 먼저 글리프를 가진 한자 — Line Seed TW, 자간 0%
 * - [DEFAULT]: 한글·영문·숫자·이모지 등 — SUIT, 자간 -1.1%
 */
enum class TextToppingScript(
    val fontFamily: FontFamily,
    val letterSpacingEm: Float,
) {
    DEFAULT(YGFontFamily.suitFontFamily, -0.011f),
    JAPANESE(lineSeedJpFontFamily, -0.011f),
    CHINESE(lineSeedTwFontFamily, 0f),
    ;

    val spanStyle: SpanStyle = SpanStyle(fontFamily = fontFamily, letterSpacing = letterSpacingEm.em)
}
