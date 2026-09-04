package com.teamyg.parfait.core.ui.reveal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchRevealStateTest {
    @Test
    fun isBatchReady_nothingLoadedYet_isNotReady() {
        // Given 조회가 오기 전이라 기다릴 대상 자체가 아직 없다
        // Then 이 순간을 완료로 세면 빗장이 먼저 풀려, 뒤늦게 온 것들이 하나씩 뜬다
        assertFalse(isBatchReady(emptyList()))
    }

    @Test
    fun isBatchReady_oneStillLoading_isNotReady() {
        assertFalse(isBatchReady(listOf(true, false, true)))
    }

    @Test
    fun isBatchReady_everyOneDone_isReady() {
        assertTrue(isBatchReady(listOf(true, true, true)))
    }

    @Test
    fun isBatchShown_nothingToWaitFor_isShown() {
        // Given 기다릴 것이 없는 빈 묶음
        // Then 가릴 이유가 없다 — 빈 화면 위에서 로딩만 계속 돌면 안 된다
        assertTrue(isBatchShown(settled = emptyList(), ready = false))
    }

    @Test
    fun isBatchShown_stillLoading_isHidden() {
        assertFalse(isBatchShown(settled = listOf(true, false), ready = false))
    }

    @Test
    fun isBatchShown_alreadyRevealed_staysShown() {
        // Given 한 번 드러낸 뒤 새 항목이 들어와 아직 안 왔다
        // Then 다시 가리지 않는다 — 하나 추가될 때마다 화면 전체가 사라지면 더 거슬린다
        assertTrue(isBatchShown(settled = listOf(true, false), ready = true))
    }
}
