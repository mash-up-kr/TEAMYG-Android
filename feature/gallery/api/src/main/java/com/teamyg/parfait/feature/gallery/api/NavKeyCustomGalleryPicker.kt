package com.teamyg.parfait.feature.gallery.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyCustomGalleryPicker(
    val recentImagePick: RecentImagePick,
    val showGuideToast: Boolean = true,
    val returnResultOnly: Boolean = false,
) : NavKey
