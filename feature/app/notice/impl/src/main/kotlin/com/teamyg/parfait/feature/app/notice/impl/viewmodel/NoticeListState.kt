package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.notice.NoticeVO

data class NoticeListState(
    val notices: List<NoticeVO> = emptyList(),
) : UiState
