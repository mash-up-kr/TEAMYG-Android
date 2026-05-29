package com.teamyg.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeySegmentation(val sourceImageUri: String) : NavKey
