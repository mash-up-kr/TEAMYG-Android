package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyCanvasBGEdit(
    val groupId: Long,
    val parfaitId: Long,
) : NavKey
