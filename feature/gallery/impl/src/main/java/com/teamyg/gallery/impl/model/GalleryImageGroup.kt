package com.teamyg.gallery.impl.model

import androidx.compose.runtime.Immutable

@Immutable
data class GalleryImageGroup(
    val date: String,
    val images: List<String>,
)
