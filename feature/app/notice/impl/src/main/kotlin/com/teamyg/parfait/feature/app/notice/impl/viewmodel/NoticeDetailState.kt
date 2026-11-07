package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.notice.NoticeVO

/**
 * @property notice `null` 은 아직 받기 전이거나 조회에 실패한 상태다.
 */
data class NoticeDetailState(
    val notice: NoticeVO? = null,
) : UiState
