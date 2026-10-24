package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.teamyg.parfait.core.designsystem.theme.typography.YGFontFamily
import com.teamyg.parfait.feature.groups.canvas.impl.R

/** 일본어 **/
private val lineSeedJpFontFamily: FontFamily = FontFamily(
    Font(R.font.line_seed_jp_regular, FontWeight.Normal),
)

/** 중국어 */
private val lineSeedTwFontFamily: FontFamily = FontFamily(
    Font(R.font.line_seed_tw_regular, FontWeight.Normal),
)

/**
 * 텍스트 토핑의 글꼴·자간을 입력 언어로 가른다(스펙 「텍스트 스타일」). Compose 편집 필드는 전체에
 * 스타일 하나만 먹으므로, 글자별이 아니라 **내용의 주된 스크립트**로 통째 고른다 — 캔버스 텍스트는
 * 보통 한 언어라 이 휴리스틱으로 충분하다.
 *
 * - [JAPANESE]: 가나(히라가나·가타카나)가 하나라도 있으면. 일본어 한자도 여기로 — Line Seed JP, 자간 -1.1%
 * - [CHINESE]: 가나는 없고 한자(漢)만 있으면. Line Seed TW, 자간 0%
 * - [DEFAULT]: 한글·영문·숫자·이모지 등 — SUIT, 자간 -1.1%
 *
 * JP·TW 는 한자 글리프를 공유해 단순 폰트 폴백으로는 구분되지 않아, 가나 유무로 일본어를 먼저 가른다.
 */
enum class TextToppingScript(
    val fontFamily: FontFamily,
    val letterSpacingEm: Float,
) {
    DEFAULT(YGFontFamily.suitFontFamily, -0.011f),
    JAPANESE(lineSeedJpFontFamily, -0.011f),
    CHINESE(lineSeedTwFontFamily, 0f),
}

private val KANA_RANGE = 0x3040..0x30FF

/** 한자: CJK 통합 한자 + 확장 A + 호환 한자 (대부분의 일/중 상용 한자를 덮는다) */
private fun isChinese(codePoint: Int): Boolean =
    codePoint in 0x4E00..0x9FFF || codePoint in 0x3400..0x4DBF || codePoint in 0xF900..0xFAFF

fun detectTextToppingScript(text: String): TextToppingScript {
    var hasChinese = false
    var offset = 0
    while (offset < text.length) {
        val codePoint = text.codePointAt(offset)
        if (codePoint in KANA_RANGE) return TextToppingScript.JAPANESE
        if (isChinese(codePoint)) hasChinese = true
        offset += Character.charCount(codePoint)
    }
    return if (hasChinese) TextToppingScript.CHINESE else TextToppingScript.DEFAULT
}
