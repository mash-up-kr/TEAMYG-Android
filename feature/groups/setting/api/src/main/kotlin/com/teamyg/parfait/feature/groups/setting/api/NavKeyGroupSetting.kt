package com.teamyg.parfait.feature.groups.setting.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyGroupSetting(
    val groupId: Long,
) : NavKey
