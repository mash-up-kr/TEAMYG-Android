package com.teamyg.parfait.domain.model

import kotlinx.datetime.LocalDate

data class GalleryImageGroup(
    val date: LocalDate,
    val images: List<String>,
)
