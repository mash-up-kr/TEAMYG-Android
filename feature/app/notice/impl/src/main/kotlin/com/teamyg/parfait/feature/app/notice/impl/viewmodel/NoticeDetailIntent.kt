package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiIntent

sealed interface NoticeDetailIntent : UiIntent {
    data object ClickBack : NoticeDetailIntent
}
