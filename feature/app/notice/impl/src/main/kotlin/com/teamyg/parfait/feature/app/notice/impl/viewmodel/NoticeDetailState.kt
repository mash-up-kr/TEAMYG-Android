package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.notice.NoticeVO

/**
 * 저장하는 것은 [notice]와 [isLoading] 둘이고, 안내 문구는 둘에서 파생한다.
 * 따로 저장하면 공지가 있는데 "볼 수 없음"인 모순된 조합이 타입상 가능해진다.
 *
 * @property isLoading 공지를 찾는 중이다 — 캐시 방출이나 다시 받기를 기다린다
 */
data class NoticeDetailState(
    val notice: NoticeVO? = null,
    val isLoading: Boolean = true,
) : UiState {
    /** 다 찾았는데도 공지가 없다 — 서버에서 내려갔거나 다시 받기에 실패했다 */
    val isUnavailable: Boolean get() = !isLoading && notice == null
}
