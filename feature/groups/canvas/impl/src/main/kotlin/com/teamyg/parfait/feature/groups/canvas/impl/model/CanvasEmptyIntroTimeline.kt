package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.animation.core.CubicBezierEasing

internal object CanvasEmptyIntroTimeline {
    const val IMAGE_COUNT = 3
    const val IMAGE_DURATION_MILLIS = 1000
    const val IMAGE_OFFSET_DP = 60
    private const val IMAGE_STEP_MILLIS = 500

    const val HIGHLIGHT_UP_MILLIS = 150
    const val HIGHLIGHT_DOWN_MILLIS = 300
    private const val HIGHLIGHT_START_MILLIS = 500
    private const val HIGHLIGHT_STEP_MILLIS = 150

    const val EXIT_DURATION_MILLIS = 300

    /** 마지막 이미지가 끝나는 시점. 강조는 이보다 먼저(2,150ms) 끝난다 */
    const val TOTAL_MILLIS = IMAGE_STEP_MILLIS * IMAGE_COUNT + IMAGE_DURATION_MILLIS

    val ImageEnterEasing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val HighlightUpEasing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val HighlightDownEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val ExitEasing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    /** 공백은 건너뛰고 글자만 센다 — 강조는 글자 단위로 이어진다 */
    fun highlightCharCount(text: String): Int = text.count { it != ' ' }

    fun imageDelayMillis(index: Int): Int = IMAGE_STEP_MILLIS * (index + 1)

    fun highlightDelayMillis(charIndex: Int): Int = HIGHLIGHT_START_MILLIS + HIGHLIGHT_STEP_MILLIS * charIndex
}
