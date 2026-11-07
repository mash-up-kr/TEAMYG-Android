package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiSideEffect

sealed interface NoticeListSideEffect : UiSideEffect {
    data object NavigateBack : NoticeListSideEffect

    data class NavigateToDetail(val id: Long) : NoticeListSideEffect

    data object ShowLoadError : NoticeListSideEffect
}
