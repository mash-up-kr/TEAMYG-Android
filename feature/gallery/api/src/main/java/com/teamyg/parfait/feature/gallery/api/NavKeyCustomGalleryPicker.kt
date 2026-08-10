package com.teamyg.parfait.feature.gallery.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyCustomGalleryPicker(
    val showGuideToast: Boolean = true,
) : NavKey
