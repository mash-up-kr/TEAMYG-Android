package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.notice.NoticeItemVO

data class NoticeListState(
    val notices: List<NoticeItemVO> = emptyList(),
) : UiState
