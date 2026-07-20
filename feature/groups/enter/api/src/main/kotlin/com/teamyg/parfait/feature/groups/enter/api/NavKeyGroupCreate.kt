package com.teamyg.parfait.feature.groups.enter.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyGroupCreate(val nickName: String) : NavKey
