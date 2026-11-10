package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiState

/**
 * @property date 표시 형식(`2026.11.17`)
 */
data class NoticeDetailState(
    val title: String,
    val date: String,
    val content: String,
) : UiState
