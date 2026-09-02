package com.teamyg.parfait.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent

private const val TRANSITION_DURATION_MILLIS = 320

/** 빠르게 출발해 천천히 멈추는 곡선 */
private val TransitionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val SlideSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = TRANSITION_DURATION_MILLIS, easing = TransitionEasing)

private val FadeSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = TRANSITION_DURATION_MILLIS, easing = TransitionEasing)

/**
 * 화면 전환 한 벌.
 *
 * 셋을 따로 넘기지 않고 한 덩어리로 묶는 이유: 들어오는 방향과 나가는 방향이 짝이 맞아야
 * 사용자에게 "덮였다 걷힌다" 하나의 동작으로 읽힌다. 한쪽만 바꾸면 오른쪽에서 들어와 놓고
 * 아래로 빠지는 식의 앞뒤 안 맞는 전환이 되기 쉽다.
 *
 * 붙이지 않은 화면은 `NavDisplay` 에 걸린 [Default] 를 쓴다.
 *
 * ```kotlin
 * entry<NavKeyPhotoViewer>(metadata = NavTransition.Fade.metadata) { ... }
 * entry<NavKeyFilter>(metadata = NavTransition.Default.copy(push = { ... }).metadata) { ... }
 * ```
 *
 * **어느 화면에 붙는지 주의** — `NavDisplay` 는 위에 놓이는 화면, 즉 새로 쌓이거나 지금 걷히는
 * 화면의 메타데이터만 본다. 그래서 A → B 전환의 모양은 B 에 붙인 것이 정하고, B 에서 A 로
 * 되돌아올 때도 마찬가지로 B 의 것이 쓰인다.
 */
data class NavTransition(
    val push: AnimatedContentTransitionScope<*>.() -> ContentTransform,
    val pop: AnimatedContentTransitionScope<*>.() -> ContentTransform,
    /** 끌어당긴 가장자리에 따라 빠지는 방향이 달라져 [pop] 과 별개로 받는다 */
    val predictivePop: AnimatedContentTransitionScope<*>.(swipeEdge: Int) -> ContentTransform,
) {
    val metadata: Map<String, Any> =
        NavDisplay.transitionSpec { push(this) } +
            NavDisplay.popTransitionSpec { pop(this) } +
            NavDisplay.predictivePopTransitionSpec { swipeEdge -> predictivePop(this, swipeEdge) }

    companion object {
        private val FadeTransform = ContentTransform(
            targetContentEnter = fadeIn(FadeSpec),
            initialContentExit = fadeOut(FadeSpec),
        )

        val Default: NavTransition = NavTransition(
            push = {
                ContentTransform(
                    targetContentEnter = slideIntoContainer(SlideDirection.Left, SlideSpec),
                    // 바로 치우면 밀고 들어오는 화면 뒤로 빈 배경이 비친다
                    initialContentExit = ExitTransition.KeepUntilTransitionsFinished,
                )
            },
            pop = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = slideOutOfContainer(SlideDirection.Right, SlideSpec),
                )
            },
            predictivePop = { swipeEdge ->
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = slideOutOfContainer(
                        towards = if (swipeEdge == NavigationEvent.EDGE_RIGHT) {
                            SlideDirection.Left
                        } else {
                            SlideDirection.Right
                        },
                        animationSpec = SlideSpec,
                    ),
                )
            },
        )

        val Fade: NavTransition = NavTransition(
            push = { FadeTransform },
            pop = { FadeTransform },
            predictivePop = { FadeTransform },
        )
    }
}
