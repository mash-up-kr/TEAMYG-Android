package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.UiIntent

sealed interface NoticeListIntent : UiIntent {
    data object ClickBack : NoticeListIntent

    data class ClickNotice(val id: Long) : NoticeListIntent
}
