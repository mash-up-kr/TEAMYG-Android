package com.teamyg.parfait.feature.gallery.impl.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.teamyg.parfait.feature.gallery.impl.model.GalleryAccessLevel

internal object GalleryPermissionManager {
    private val primaryPermission: String = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_IMAGES
        else -> Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val requiredPermissions: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )

        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }

    fun hasFullAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            isGranted(
                context = context,
                permission = Manifest.permission.READ_MEDIA_IMAGES,
            )

        else ->
            isGranted(
                context = context,
                permission = Manifest.permission.READ_EXTERNAL_STORAGE,
            )
    }

    fun hasPartialAccess(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        isGranted(
            context = context,
            permission = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) &&
        !isGranted(
            context = context,
            permission = Manifest.permission.READ_MEDIA_IMAGES,
        )

    fun shouldShowRationale(activity: Activity): Boolean = ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        primaryPermission,
    )

    fun resolveAccessLevelOnEnter(context: Context): GalleryAccessLevel = when {
        hasFullAccess(context) -> GalleryAccessLevel.FULL
        hasPartialAccess(context) -> GalleryAccessLevel.PARTIAL
        else -> GalleryAccessLevel.DENIED
    }

    fun resolveAccessLevelAfterRequest(
        context: Context,
        activity: Activity?,
    ): GalleryAccessLevel {
        if (hasFullAccess(context)) {
            return GalleryAccessLevel.FULL
        }

        if (hasPartialAccess(context)) {
            return GalleryAccessLevel.PARTIAL
        }

        val canRetry = activity?.let { shouldShowRationale(it) } ?: true

        return if (canRetry) GalleryAccessLevel.DENIED else GalleryAccessLevel.PERMANENTLY_DENIED
    }

    private fun isGranted(
        context: Context,
        permission: String,
    ): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
