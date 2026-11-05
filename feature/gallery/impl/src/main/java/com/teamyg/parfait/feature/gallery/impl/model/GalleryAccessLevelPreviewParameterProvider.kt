package com.teamyg.parfait.feature.gallery.impl.model

import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.util.android.permission.GalleryPermissionManager

internal class GalleryAccessLevelPreviewParameterProvider :
    NamedPreviewParameterProvider<GalleryPermissionManager.GalleryAccessLevel>() {
    override val entries = listOf(
        "denied" to GalleryPermissionManager.GalleryAccessLevel.DENIED,
        "permanentlyDenied" to GalleryPermissionManager.GalleryAccessLevel.PERMANENTLY_DENIED,
        "partial" to GalleryPermissionManager.GalleryAccessLevel.PARTIAL,
        "full" to GalleryPermissionManager.GalleryAccessLevel.FULL,
    )
}
