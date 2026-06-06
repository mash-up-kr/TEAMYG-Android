package com.teamyg.gallery.impl.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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

    private fun isGranted(
        context: Context,
        permission: String,
    ): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
