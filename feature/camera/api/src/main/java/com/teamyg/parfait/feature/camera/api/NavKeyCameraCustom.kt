package com.teamyg.parfait.feature.camera.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyCameraCustom(
    val showGuideToast: Boolean = true,
    val returnResultOnly: Boolean = false,
) : NavKey
