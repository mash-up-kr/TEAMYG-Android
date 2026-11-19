package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.domain.model.notice.NoticeVO
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoticeDetailStateTest {
    private val notice = NoticeVO(
        id = 1L,
        title = "공지",
        publishedDate = LocalDate(2026, 11, 12),
        content = "본문",
    )

    @Test
    fun initialState_isLoadingAndNotUnavailable() {
        // Given, When 아무것도 찾기 전의 기본 상태
        val state = NoticeDetailState()

        // Then 찾는 중이라 안내 문구를 띄울 상태가 아니다
        assertTrue(state.isLoading)
        assertFalse(state.isUnavailable)
    }

    @Test
    fun finishedWithoutNotice_isUnavailable() {
        // Given, When 다 찾았는데 공지가 없다
        val state = NoticeDetailState(isLoading = false)

        // Then 볼 수 없는 공지다
        assertTrue(state.isUnavailable)
    }

    @Test
    fun finishedWithNotice_isNotUnavailable() {
        // Given, When 공지를 찾았다
        val state = NoticeDetailState(notice = notice, isLoading = false)

        // Then 안내 문구는 필요 없다
        assertFalse(state.isUnavailable)
    }
}
