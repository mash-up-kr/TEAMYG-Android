package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.domain.model.notice.NoticeVO

sealed interface NoticeListSideEffect : UiSideEffect {
    data object NavigateBack : NoticeListSideEffect

    data class NavigateToDetail(val notice: NoticeVO) : NoticeListSideEffect

    data object ShowLoadError : NoticeListSideEffect
}
