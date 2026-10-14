package com.teamyg.parfait.core.util.android.model

import kotlin.math.roundToInt

internal data class DecodeSize(
    val width: Int,
    val height: Int,
) {
    companion object {
        /**
         * 디코드한 비트맵의 긴 변 상한(px).
         *
         * 소프트웨어 비트맵은 100MB 를 넘으면 그리는 순간 `RecordingCanvas` 가 던진다. 값을 고른 근거는
         * `docs/adr/0012-mlkit-subject-segmentation.md` 「원본 디코드 상한」에 있다.
         */
        const val LONG_SIDE_LIMIT = 4096

        fun capped(
            width: Int,
            height: Int,
        ): DecodeSize {
            val longSide = maxOf(width, height)
            if (longSide <= LONG_SIDE_LIMIT) return DecodeSize(width, height)

            val ratio = LONG_SIDE_LIMIT.toDouble() / longSide

            // 0 은 디코더가 던진다
            return DecodeSize(
                width = (width * ratio).roundToInt().coerceAtLeast(1),
                height = (height * ratio).roundToInt().coerceAtLeast(1),
            )
        }
    }
}
