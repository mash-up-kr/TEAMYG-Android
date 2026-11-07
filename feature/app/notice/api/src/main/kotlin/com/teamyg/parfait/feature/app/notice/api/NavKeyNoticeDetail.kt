package com.teamyg.parfait.feature.app.notice.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyNoticeDetail(
    val noticeId: Long,
) : NavKey
