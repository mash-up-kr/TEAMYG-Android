package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

internal enum class GroupListEmptyIntroPhase {
    Entering,
    Shown,
    Dismissing,
    Dismissed,
}

/** 등장이 끝나기 전의 터치는 무시한다. */
internal fun GroupListEmptyIntroPhase.onTouchDown(): GroupListEmptyIntroPhase = when (this) {
    GroupListEmptyIntroPhase.Shown -> GroupListEmptyIntroPhase.Dismissing
    else -> this
}

/** 진행 중인 애니메이션을 끝 상태로 건너뛴다. */
internal fun GroupListEmptyIntroPhase.onStop(): GroupListEmptyIntroPhase = when (this) {
    GroupListEmptyIntroPhase.Entering -> GroupListEmptyIntroPhase.Shown
    GroupListEmptyIntroPhase.Dismissing -> GroupListEmptyIntroPhase.Dismissed
    else -> this
}

internal fun GroupListEmptyIntroPhase.enterValue(animated: Float): Float = when (this) {
    GroupListEmptyIntroPhase.Entering -> animated
    else -> 1f
}

internal fun GroupListEmptyIntroPhase.exitValue(animated: Float): Float = when (this) {
    GroupListEmptyIntroPhase.Entering, GroupListEmptyIntroPhase.Shown -> 1f
    GroupListEmptyIntroPhase.Dismissing -> animated
    GroupListEmptyIntroPhase.Dismissed -> 0f
}

internal object GroupListEmptyIntroTimeline {
    const val INITIAL_DELAY_MILLIS = 500
    const val DUMMY_STAGGER_MILLIS = 500
    const val DUMMY_DURATION_MILLIS = 1000
    const val TOOLTIP_DELAY_MILLIS = 2500
    const val TOOLTIP_DURATION_MILLIS = 500
    const val ENTER_TOTAL_MILLIS = 3000
    const val EXIT_DURATION_MILLIS = 300

    fun dummyDelayMillis(index: Int): Int = INITIAL_DELAY_MILLIS + DUMMY_STAGGER_MILLIS * index

    val DummyEnterEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val ExitEasing: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    /**
     * Figma 노드 `5417:6432`의 스프링(mass 1, stiffness 80, damping 20)을 500ms에 맞춰 내보낸 곡선이다.
     * 마지막 샘플이 1에 못 미쳐 끝 지점은 1로 고정한다.
     */
    val TooltipEnterEasing: Easing = Easing { fraction ->
        when {
            fraction <= 0f -> 0f

            fraction >= 1f -> 1f

            else -> {
                val position = fraction * (TooltipSamples.size - 1)
                val lower = position.toInt()
                val t = position - lower
                TooltipSamples[lower] + (TooltipSamples[lower + 1] - TooltipSamples[lower]) * t
            }
        }
    }

    private val TooltipSamples = floatArrayOf(
        0f, 0.0216f, 0.0747f, 0.1458f, 0.2255f, 0.3076f, 0.3879f, 0.4638f, 0.5339f, 0.5974f,
        0.6542f, 0.7044f, 0.7484f, 0.7866f, 0.8196f, 0.8479f, 0.8722f, 0.8928f, 0.9103f, 0.9251f,
        0.9375f, 0.948f, 0.9568f, 0.9642f, 0.9703f, 0.9754f, 0.9797f, 0.9832f, 0.9862f, 0.9886f,
        0.9906f, 0.9923f, 0.9936f, 0.9948f, 0.9957f, 0.9965f, 0.9971f, 0.9976f, 0.9981f, 0.9984f,
        0.9987f, 0.9989f, 0.9991f, 0.9993f, 0.9994f, 0.9995f, 0.9996f, 0.9997f, 0.9997f, 0.9998f,
        0.9998f,
    )
}
