package com.teamyg.parfait.feature.gallery.impl.model

enum class GalleryAccessLevel {
    INITIAL,
    DENIED,
    PERMANENTLY_DENIED,
    PARTIAL,
    FULL,
    ;

    val isInit: Boolean
        get() = this == INITIAL

    val isPartial: Boolean
        get() = this == PARTIAL

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
