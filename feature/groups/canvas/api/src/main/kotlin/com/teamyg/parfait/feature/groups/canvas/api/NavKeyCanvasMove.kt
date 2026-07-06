package com.teamyg.parfait.feature.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyCanvasMove(val imageUri: String) : NavKey
