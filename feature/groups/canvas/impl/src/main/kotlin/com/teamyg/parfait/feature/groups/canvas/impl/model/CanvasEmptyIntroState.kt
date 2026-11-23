package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.EXIT_DURATION_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.ExitEasing
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HIGHLIGHT_DOWN_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HIGHLIGHT_UP_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HighlightDownEasing
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HighlightUpEasing
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.IMAGE_COUNT
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.IMAGE_DURATION_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.ImageEnterEasing
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.highlightDelayMillis
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.imageDelayMillis
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 값 getter 가 단계를 거치므로 단계만 바뀌어도 화면이 완료·닫힘 상태가 된다. 멈추지 못한 코루틴이
 * 남아도 단계가 값을 덮는다. 단계 자체는 ViewModel 이 들고 있어, 다른 화면에 다녀와 이 홀더가 새로
 * 만들어져도 `Shown` 에서 이어진다.
 */
@Stable
internal class CanvasEmptyIntroState(
    highlightCharCount: Int,
    private val phase: () -> CanvasEmptyIntroPhase,
) {
    private val images = List(IMAGE_COUNT) { Animatable(0f) }
    private val highlights = List(highlightCharCount) { Animatable(0f) }
    private val exit = Animatable(1f)

    fun imageProgress(index: Int): Float = phase().enterValue(images[index].value)

    /** 글자색이 `Gray500 → Black` 으로 얼마나 갔는지. 닫히는 동안은 현재 값에서 함께 사라진다 */
    fun highlight(charIndex: Int): Float = when (phase()) {
        CanvasEmptyIntroPhase.Entering -> highlights[charIndex].value
        CanvasEmptyIntroPhase.Dismissing -> highlights[charIndex].value * exitAlpha
        else -> 0f
    }

    val exitAlpha: Float get() = phase().exitValue(exit.value)

    suspend fun play() {
        coroutineScope {
            images.forEachIndexed { index, image ->
                launch {
                    delay(imageDelayMillis(index).toLong())
                    image.animateTo(1f, tween(IMAGE_DURATION_MILLIS, easing = ImageEnterEasing))
                }
            }
            highlights.forEachIndexed { index, highlight ->
                launch {
                    delay(highlightDelayMillis(index).toLong())
                    highlight.animateTo(1f, tween(HIGHLIGHT_UP_MILLIS, easing = HighlightUpEasing))
                    highlight.animateTo(0f, tween(HIGHLIGHT_DOWN_MILLIS, easing = HighlightDownEasing))
                }
            }
        }
    }

    /** 재생을 건너뛰고 완료 상태가 됐을 때, 이어지는 디졸브가 이 값에서 출발하도록 맞춘다 */
    suspend fun settle() {
        images.forEach { it.snapTo(1f) }
        highlights.forEach { it.snapTo(0f) }
    }

    suspend fun dismiss() {
        exit.animateTo(0f, tween(EXIT_DURATION_MILLIS, easing = ExitEasing))
    }
}

@Composable
internal fun rememberCanvasEmptyIntroState(
    highlightCharCount: Int,
    phase: CanvasEmptyIntroPhase,
    isPaused: Boolean,
    onAnimationFinished: () -> Unit,
    onDismissFinished: () -> Unit,
): CanvasEmptyIntroState {
    val currentPhase by rememberUpdatedState(phase)
    val currentOnAnimationFinished by rememberUpdatedState(onAnimationFinished)
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val state = remember(highlightCharCount) { CanvasEmptyIntroState(highlightCharCount) { currentPhase } }

    LaunchedEffect(state, phase, isPaused) {
        when (phase) {
            // 로딩 덮개 아래에서 시작하면 앞부분이 가려지므로 덮개가 걷힌 뒤에 재생한다
            CanvasEmptyIntroPhase.Entering -> if (isPaused.not()) {
                state.play()
                currentOnAnimationFinished()
            }

            CanvasEmptyIntroPhase.Shown -> state.settle()

            CanvasEmptyIntroPhase.Dismissing -> {
                state.dismiss()
                currentOnDismissFinished()
            }

            else -> Unit
        }
    }

    return state
}
