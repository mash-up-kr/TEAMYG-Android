package com.teamyg.parfait.feature.gallery.impl.model

enum class GalleryAccessLevel {
    INITIAL,
    DENIED,
    PERMANENTLY_DENIED,
    PARTIAL,
    FULL,
    ;

    val isPartial: Boolean
        get() = when (this) {
            PARTIAL -> true
            else -> false
        }

    val hasPermission: Boolean
        get() = when (this) {
            PARTIAL,
            FULL,
            -> true

            else -> false
        }

    val isDeniedPermission: Boolean
        get() = when (this) {
            DENIED,
            PERMANENTLY_DENIED,
            -> true

            else -> false
        }
}
