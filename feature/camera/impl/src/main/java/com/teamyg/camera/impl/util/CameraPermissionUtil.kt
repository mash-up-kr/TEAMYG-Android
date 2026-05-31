package com.teamyg.camera.impl.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat

internal object CameraPermissionUtil {
    fun shouldShowRationale(
        activity: Activity?,
        permission: String,
    ): Boolean =
        activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this

    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }

    return null
}
