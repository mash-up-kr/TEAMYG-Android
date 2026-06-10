package com.teamyg.gallery.impl.model

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

internal class GalleryAccessLevelPreviewParameterProvider : PreviewParameterProvider<GalleryAccessLevel> {
    override val values: Sequence<GalleryAccessLevel>
        get() = sequenceOf(
            GalleryAccessLevel.DENIED,
            GalleryAccessLevel.PERMANENTLY_DENIED,
            GalleryAccessLevel.PARTIAL,
            GalleryAccessLevel.FULL,
        )
}
