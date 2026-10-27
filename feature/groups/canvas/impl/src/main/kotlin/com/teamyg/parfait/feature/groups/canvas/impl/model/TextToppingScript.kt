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
 * 텍스트 토핑의 글꼴·자간을 글자 구간마다 고른다 — 분할 규칙은 [textToppingScriptRuns].
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

private fun isKana(codePoint: Int): Boolean =
    codePoint in 0x3040..0x30FF || codePoint in 0x31F0..0x31FF || codePoint in 0xFF66..0xFF9F

/** 반복 부호 `々〆〇`와 `〻`도 한자로 본다. tools/text-topping-han-coverage/generate.py 의 범위와 같아야 한다. */
private fun isHan(codePoint: Int): Boolean = codePoint in 0x3005..0x3007 ||
    codePoint == 0x303B ||
    codePoint in 0x3400..0x4DBF ||
    codePoint in 0x4E00..0x9FFF ||
    codePoint in 0xF900..0xFAFF ||
    codePoint in 0x20000..0x3FFFF

/** 전각 숫자·라틴(`２` `Ａ` `ａ`)은 글자 폰트를 정하지 않고 주변 구간을 따른다. */
private fun isFullwidthAlphanumeric(codePoint: Int): Boolean =
    codePoint in 0xFF10..0xFF19 || codePoint in 0xFF21..0xFF3A || codePoint in 0xFF41..0xFF5A

/**
 * 한자 우선순위. 열거형 상수를 최상위 프로퍼티에 담지 않고 호출 때마다 만든다 — 열거형 초기화가 이 파일의
 * 최상위 폰트 패밀리를 읽어서, 최상위에서 상수를 잡으면 클래스 초기화 순서에 따라 null 이 새어 든다.
 */
private fun hanPriority(hasKana: Boolean): List<Pair<TextToppingScript, TextToppingHanCoverage>> {
    val japanese = TextToppingScript.JAPANESE to LINE_SEED_JP_HAN
    val chinese = TextToppingScript.CHINESE to LINE_SEED_TW_HAN
    return if (hasKana) listOf(japanese, chinese) else listOf(chinese, japanese)
}

/**
 * 글자마다 글꼴을 골라 같은 글꼴이 이어지는 구간으로 합친다.
 *
 * 가나 유무로 한자 우선순위를 정한다. 상용 한자는 두 폰트에 다 있어서, 먼저 오는 폰트가 상대 언어의
 * 한자까지 가져가기 때문이다. 가나는 TW 에도 글리프가 있으나 일본어 글꼴로 그리도록 JP 로 명시한다.
 *
 * 공백·문장부호·이모지·ZWJ·변형 선택자·전각 숫자와 라틴 같은 중립 문자는 앞 구간을 따르고, 맨 앞의 중립 문자는 첫
 * 비중립 글자를 따른다. 중립뿐이면 [TextToppingScript.DEFAULT] 한 구간이다.
 */
internal fun textToppingScriptRuns(text: CharSequence): List<TextToppingScriptRun> {
    if (text.isEmpty()) return emptyList()
    val hanOrder = hanPriority(hasKana = text.containsKana())
    val runs = mutableListOf<TextToppingScriptRun>()
    var current: TextToppingScript? = null
    var runStart = 0
    var offset = 0
    while (offset < text.length) {
        val codePoint = Character.codePointAt(text, offset)
        val script = when {
            isKana(codePoint) -> TextToppingScript.JAPANESE
            isHan(codePoint) -> (hanOrder.firstOrNull { codePoint in it.second } ?: hanOrder.first()).first
            isFullwidthAlphanumeric(codePoint) -> null
            Character.isLetter(codePoint) || Character.isDigit(codePoint) -> TextToppingScript.DEFAULT
            else -> null
        }
        if (script != null) {
            if (current != null && script != current) {
                runs += TextToppingScriptRun(current, runStart, offset)
                runStart = offset
            }
            current = script
        }
        offset += Character.charCount(codePoint)
    }
    runs += TextToppingScriptRun(current ?: TextToppingScript.DEFAULT, runStart, text.length)
    return runs
}

private fun CharSequence.containsKana(): Boolean {
    var offset = 0
    while (offset < length) {
        val codePoint = Character.codePointAt(this, offset)
        if (isKana(codePoint)) return true
        offset += Character.charCount(codePoint)
    }
    return false
}
