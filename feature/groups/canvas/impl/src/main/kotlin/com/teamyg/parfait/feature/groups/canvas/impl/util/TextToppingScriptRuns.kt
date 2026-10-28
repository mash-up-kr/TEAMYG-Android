package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.LINE_SEED_JP_CHINESE_CHARACTERS
import com.teamyg.parfait.feature.groups.canvas.impl.model.LINE_SEED_TW_CHINESE_CHARACTERS
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_CHINESE_CHARACTER_RANGES
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingChineseCharacterCoverage
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScriptRun

private class ChineseCharacterFont(val script: TextToppingScript, val coverage: TextToppingChineseCharacterCoverage)

private val JAPANESE_FIRST: List<ChineseCharacterFont> = listOf(
    ChineseCharacterFont(TextToppingScript.JAPANESE, LINE_SEED_JP_CHINESE_CHARACTERS),
    ChineseCharacterFont(TextToppingScript.CHINESE, LINE_SEED_TW_CHINESE_CHARACTERS),
)

private val CHINESE_FIRST: List<ChineseCharacterFont> = JAPANESE_FIRST.reversed()

/** 우선순위 순서로 글리프를 가진 첫 폰트를 고르고, 아무도 없으면 1순위 폰트로 떨어진다. */
private fun List<ChineseCharacterFont>.scriptFor(codePoint: Int): TextToppingScript =
    (firstOrNull { codePoint in it.coverage } ?: first()).script

private fun isKana(codePoint: Int): Boolean =
    codePoint in 0x3040..0x30FF || codePoint in 0x31F0..0x31FF || codePoint in 0xFF66..0xFF9F

/** 범위는 커버리지 테이블과 같은 생성물에서 온다 — 따로 적으면 어긋난 한자가 말없이 1순위 폰트로 빠진다. */
private fun isChineseCharacter(codePoint: Int): Boolean = TEXT_TOPPING_CHINESE_CHARACTER_RANGES.any { codePoint in it }

/** 전각 숫자·라틴(`２` `Ａ` `ａ`)은 글자 폰트를 정하지 않고 주변 구간을 따른다. */
private fun isFullwidthAlphanumeric(codePoint: Int): Boolean =
    codePoint in 0xFF10..0xFF19 || codePoint in 0xFF21..0xFF3A || codePoint in 0xFF41..0xFF5A

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
    val chineseCharacterOrder = if (text.containsKana()) JAPANESE_FIRST else CHINESE_FIRST
    val runs = mutableListOf<TextToppingScriptRun>()
    var current: TextToppingScript? = null
    var runStart = 0
    var offset = 0
    while (offset < text.length) {
        val codePoint = Character.codePointAt(text, offset)
        val script = when {
            isKana(codePoint) -> TextToppingScript.JAPANESE
            isChineseCharacter(codePoint) -> chineseCharacterOrder.scriptFor(codePoint)
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
