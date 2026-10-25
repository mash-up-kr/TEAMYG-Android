package com.teamyg.parfait.core.util.jvm.extension

private const val ZERO_WIDTH_JOINER = 0x200D
private const val VARIATION_SELECTOR_15 = 0xFE0E
private const val VARIATION_SELECTOR_16 = 0xFE0F
private val EMOJI_MODIFIER_RANGE = 0x1F3FB..0x1F3FF

// JDK의 java.text.BreakIterator는 ZWJ 결합 이모지(👨‍👩‍👧)와 스킨톤 수식자(🙌🏽)를 그래핌
// 클러스터 하나로 묶지 못한다(실측 확인). 플랫폼 구현에 기대지 않고 필요한 접합 규칙만 직접 둔다.
private fun isCombiningMark(codePoint: Int): Boolean {
    val type = Character.getType(codePoint)
    return type == Character.NON_SPACING_MARK.toInt() ||
        type == Character.COMBINING_SPACING_MARK.toInt() ||
        type == Character.ENCLOSING_MARK.toInt()
}

private fun attachesToPreviousCluster(
    codePoint: Int,
    previousCodePoint: Int,
): Boolean = previousCodePoint == ZERO_WIDTH_JOINER ||
    codePoint == ZERO_WIDTH_JOINER ||
    codePoint == VARIATION_SELECTOR_15 ||
    codePoint == VARIATION_SELECTOR_16 ||
    codePoint in EMOJI_MODIFIER_RANGE ||
    isCombiningMark(codePoint)

private fun String.graphemeClusterStartOffsets(): List<Int> {
    if (isEmpty()) return emptyList()
    val starts = mutableListOf(0)
    var previousCodePoint = codePointAt(0)
    var offset = Character.charCount(previousCodePoint)
    while (offset < length) {
        val codePoint = codePointAt(offset)
        if (!attachesToPreviousCluster(codePoint, previousCodePoint)) starts.add(offset)
        previousCodePoint = codePoint
        offset += Character.charCount(codePoint)
    }
    return starts
}

/** 화면에 보이는 글자 단위로 센다 — 조합 이모지(🙌🏽, 👨‍👩‍👧 등)와 줄바꿈도 1자다 */
fun String.graphemeClusterCount(): Int = graphemeClusterStartOffsets().size

/** 클러스터 경계에서만 자르므로 조합 이모지가 중간에서 깨지지 않는다 */
fun String.truncateToGraphemeClusters(maxClusters: Int): String {
    val starts = graphemeClusterStartOffsets()
    if (starts.size <= maxClusters) return this
    return substring(0, starts[maxClusters])
}

/** 마지막 그래핌 클러스터(화면에 보이는 마지막 한 글자). 빈 문자열이면 null */
fun String.lastGraphemeCluster(): String? {
    val starts = graphemeClusterStartOffsets()
    if (starts.isEmpty()) return null
    return substring(starts.last())
}
