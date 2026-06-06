package com.teamyg.gallery.impl.model

import android.app.Activity
import android.content.Context
import com.teamyg.gallery.impl.utils.GalleryPermissionManager

enum class GalleryAccessLevel {
    INITIAL,
    DENIED,
    PERMANENTLY_DENIED,
    PARTIAL,
    FULL,
    ;

    companion object {
        internal fun resolveAccessLevelOnEnter(context: Context): GalleryAccessLevel = when {
            GalleryPermissionManager.hasFullAccess(context) -> FULL
            GalleryPermissionManager.hasPartialAccess(context) -> PARTIAL
            else -> DENIED
        }

        internal fun resolveAccessLevelAfterRequest(
            context: Context,
            activity: Activity?,
        ): GalleryAccessLevel {
            if (GalleryPermissionManager.hasFullAccess(context)) {
                return FULL
            }

            if (GalleryPermissionManager.hasPartialAccess(context)) {
                return PARTIAL
            }

            val canRetry = activity?.let { GalleryPermissionManager.shouldShowRationale(it) } ?: true

            return if (canRetry) DENIED else PERMANENTLY_DENIED
        }
    }
}
