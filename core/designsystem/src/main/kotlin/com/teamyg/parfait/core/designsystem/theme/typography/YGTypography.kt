package com.teamyg.parfait.core.designsystem.theme.typography

import androidx.compose.runtime.Immutable

@Immutable
data class YGTypography(
    val title: YGTypographyTitle,
    val body: YGTypographyBody,
    val caption: YGTypographyCaption,
)
