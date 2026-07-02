package com.teamyg.parfait.feature.groups.home.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyGroupHome(val groupId: Int) : NavKey
