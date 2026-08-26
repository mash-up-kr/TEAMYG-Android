package com.teamyg.parfait.feature.groups.canvas.impl.util

/**
 * 다운스케일은 블록 평균이라, 0보다 크기만 하면 불투명으로 치면 원본 블록에 픽셀 하나만 있어도
 * 마스크가 채워져 실루엣이 한 픽셀만큼 부푼다. 절반을 기준으로 삼아 부풀림과 깎임을 상쇄한다.
 */
const val TOPPING_MASK_ALPHA_THRESHOLD = 128

private const val BITS_PER_WORD = 64

/** 토핑 누끼에서 불투명한 자리만 남긴 저해상도 마스크. 판정에만 쓰고 그리지 않는다. */
class ToppingAlphaMask internal constructor(
    val width: Int,
    val height: Int,
    private val bits: LongArray,
) {
    /** 마스크 밖 좌표는 예외가 아니라 투명으로 답한다 — 테두리 되밀기 점이 정의상 밖으로 나간다. */
    fun isOpaqueAt(
        x: Int,
        y: Int,
    ): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        val index = y * width + x
        return bits[index / BITS_PER_WORD] and (1L shl (index % BITS_PER_WORD)) != 0L
    }

    val hasAnyOpaque: Boolean
        get() = bits.any { it != 0L }
}

fun toppingAlphaMaskOf(
    width: Int,
    height: Int,
    alphaAt: (x: Int, y: Int) -> Int,
): ToppingAlphaMask {
    val bits = LongArray((width * height + BITS_PER_WORD - 1) / BITS_PER_WORD)

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (alphaAt(x, y) >= TOPPING_MASK_ALPHA_THRESHOLD) {
                val index = y * width + x
                bits[index / BITS_PER_WORD] =
                    bits[index / BITS_PER_WORD] or (1L shl (index % BITS_PER_WORD))
            }
        }
    }

    return ToppingAlphaMask(width = width, height = height, bits = bits)
}
