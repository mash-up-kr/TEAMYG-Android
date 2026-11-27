package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasEmptyIntroStateTest {
    private var phase = CanvasEmptyIntroPhase.Entering
    private val state = CanvasEmptyIntroState(highlightCharCount = 9) { phase }

    @Test
    fun reset_afterAFullPlayAndDismiss_returnsToTheStartOfTheIntro() = runTest {
        // Given 어제의 연출을 끝까지 재생하고 닫은 홀더 — 03시 뒤에도 같은 컴포지션이 이 홀더를 들고 있다
        withContext(FakeFrameClock()) {
            state.play()
            state.dismiss()
        }

        // When 새 날짜의 연출을 시작하려고 되돌린다
        state.reset()

        // Then 이미지는 위에서 내려오기 전, 닫힘 알파는 온전한 상태라 처음부터 다시 재생된다
        assertEquals(0f, state.imageProgress(0))
        assertEquals(0f, state.imageProgress(2))
        phase = CanvasEmptyIntroPhase.Dismissing
        assertEquals(1f, state.exitAlpha)
    }

    @Test
    fun reset_afterSettle_returnsImagesToTheirStart() = runTest {
        // Given 재생을 건너뛰어 완료 상태로 맞춘 홀더
        state.settle()

        // When 되돌린다
        state.reset()

        // Then 이미지가 다시 위에서 내려올 수 있다
        assertEquals(0f, state.imageProgress(1))
    }
}

/** 프레임마다 16ms 가 흐르는 시계 — 가상 시간이라 실제로 기다리지 않고 애니메이션이 끝까지 간다 */
private class FakeFrameClock : MonotonicFrameClock {
    private var nanos = 0L

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        nanos += 16_000_000L
        return onFrame(nanos)
    }
}
