package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 폰트가 글리프를 가진 한자 코드포인트 집합.
 *
 * BMP 구간을 문자열로 받는 이유는 `intArrayOf` 로는 JVM 메서드 64KB 한도를 넘기 때문이다.
 *
 * @param bmpPairs 정렬된 `[start, end]` 문자 쌍을 이어 붙인 문자열
 * @param supplementaryPairs 같은 구조의 정수 배열(U+10000 이상)
 */
internal class TextToppingChineseCharacterCoverage(
    private val bmpPairs: String,
    private val supplementaryPairs: IntArray,
) {
    operator fun contains(codePoint: Int): Boolean = if (codePoint <= 0xFFFF) {
        containsIn(bmpPairs.length / 2, { bmpPairs[it].code }, codePoint)
    } else {
        containsIn(supplementaryPairs.size / 2, { supplementaryPairs[it] }, codePoint)
    }

    private inline fun containsIn(
        pairCount: Int,
        at: (Int) -> Int,
        codePoint: Int,
    ): Boolean {
        var low = 0
        var high = pairCount - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            when {
                codePoint < at(mid * 2) -> high = mid - 1
                codePoint > at(mid * 2 + 1) -> low = mid + 1
                else -> return true
            }
        }
        return false
    }
}
