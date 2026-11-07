package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiSideEffect

sealed interface NoticeDetailSideEffect : UiSideEffect {
    data object NavigateBack : NoticeDetailSideEffect

    data object ShowLoadError : NoticeDetailSideEffect
}
