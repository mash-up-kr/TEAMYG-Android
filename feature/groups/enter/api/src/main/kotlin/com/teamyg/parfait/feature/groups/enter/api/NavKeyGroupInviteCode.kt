package com.teamyg.parfait.feature.groups.enter.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** @property inviteCode App Links 로 들어왔을 때 입력칸을 미리 채울 코드. 수동 진입이면 `null`. */
@Serializable
data class NavKeyGroupInviteCode(val inviteCode: String? = null) : NavKey
