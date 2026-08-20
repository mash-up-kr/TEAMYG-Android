package com.teamyg.parfait.core.navigation

import androidx.navigation3.runtime.get
import androidx.navigation3.ui.NavDisplay
import kotlin.test.Test
import kotlin.test.assertNotNull

class NavTransitionTest {
    private val presets = mapOf(
        "Default" to NavTransition.Default,
        "Fade" to NavTransition.Fade,
    )

    @Test
    fun metadata_everyPreset_fillsAllThreeSlots() {
        presets.forEach { (name, transition) ->
            // Given 프리셋 하나
            val metadata = transition.metadata

            // Then 쌓임·걷힘·뒤로가기 제스처가 모두 채워져 있다 — 하나라도 비면 그 방향만
            // 라이브러리 기본 전환(fade + scaleOut)으로 튀어 앞뒤가 안 맞는 화면이 된다
            assertNotNull(metadata[NavDisplay.TransitionKey], "$name 의 push 가 비어 있다")
            assertNotNull(metadata[NavDisplay.PopTransitionKey], "$name 의 pop 이 비어 있다")
            assertNotNull(
                metadata[NavDisplay.PredictivePopTransitionKey],
                "$name 의 predictivePop 이 비어 있다",
            )
        }
    }

    @Test
    fun metadata_afterCopyingOneSlot_stillFillsAllThreeSlots() {
        // Given 들어오는 모양만 바꾼 전환
        val custom = NavTransition.Default.copy(push = { NavTransition.Fade.push(this) })

        // When metadata 로 만든다
        val metadata = custom.metadata

        // Then 손대지 않은 나머지는 기본 전환 그대로 실려 나간다
        assertNotNull(metadata[NavDisplay.TransitionKey])
        assertNotNull(metadata[NavDisplay.PopTransitionKey])
        assertNotNull(metadata[NavDisplay.PredictivePopTransitionKey])
    }
}
