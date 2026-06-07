package com.teamyg.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NavKeyCanvasEdit(val imageUri: String) : NavKey
