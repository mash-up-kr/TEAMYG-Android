package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.notice.NoticeVO

/**
 * 둘 다 기본값이면 공지를 찾는 중이다.
 *
 * @property notice 캐시에서 찾은 공지
 * @property isUnavailable 공지를 보여 줄 수 없다 — 서버에서 내려간 공지이거나 다시 받기에 실패했다
 */
data class NoticeDetailState(
    val notice: NoticeVO? = null,
    val isUnavailable: Boolean = false,
) : UiState
