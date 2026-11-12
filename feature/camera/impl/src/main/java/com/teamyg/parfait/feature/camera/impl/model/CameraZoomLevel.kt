package com.teamyg.parfait.feature.camera.impl.model

import kotlin.math.roundToInt

/**
 * 줌 버튼이 순환하는 배율 단계. 선언 순서가 순환 순서다(Figma `BL_06`: 1x > 2x > 0.5x).
 * 핀치 배율은 소수 첫째 자리로 반올림해 단계와 맞춘다 — 라벨이 `2x`면 2x 단계다.
 */
enum class CameraZoomLevel(
    val ratio: Float,
) {
    ONE(ratio = 1f),
    TWO(ratio = 2f),
    HALF(ratio = 0.5f),
    ;

    fun next(available: List<CameraZoomLevel>): CameraZoomLevel {
        val index = available.indexOf(this)
        if (index == -1) return ONE

        return available[(index + 1) % available.size]
    }

    companion object {
        fun available(
            range: ClosedFloatingPointRange<Float>,
            halfZoomSource: HalfZoomSource,
        ): List<CameraZoomLevel> = entries.filter { level ->
            when (level) {
                HALF -> halfZoomSource != HalfZoomSource.NONE
                else -> level.ratio in range
            }
        }

        /** 핀치로 단계 사이에 멈췄으면 `null` */
        fun matching(
            ratio: Float,
            available: List<CameraZoomLevel>,
        ): CameraZoomLevel? = available.firstOrNull { it.ratio.toTenths() == ratio.toTenths() }

        /** `1x` `1.4x` `0.5x` */
        fun labelOf(ratio: Float): String {
            val tenths = ratio.toTenths()
            val whole = tenths / 10
            val fraction = tenths % 10
            return if (fraction == 0) "${whole}x" else "$whole.${fraction}x"
        }

        private fun Float.toTenths(): Int = (this * 10).roundToInt()
    }
}
