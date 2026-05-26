package com.teamyg.grouphome.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavigationKey(val groupId: Int) : NavKey
