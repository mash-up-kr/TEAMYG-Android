package com.teamyg.parfait.feature.gallery.impl.model

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.util.permission.GalleryPermissionManager

internal class GalleryAccessLevelPreviewParameterProvider :
    PreviewParameterProvider<GalleryPermissionManager.GalleryAccessLevel> {
    override val values: Sequence<GalleryPermissionManager.GalleryAccessLevel>
        get() = sequenceOf(
            GalleryPermissionManager.GalleryAccessLevel.DENIED,
            GalleryPermissionManager.GalleryAccessLevel.PERMANENTLY_DENIED,
            GalleryPermissionManager.GalleryAccessLevel.PARTIAL,
            GalleryPermissionManager.GalleryAccessLevel.FULL,
        )
}
