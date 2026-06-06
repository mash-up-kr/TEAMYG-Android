package com.teamyg.gallery.impl.utils.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun Context.buildAppSettingsIntent(): Intent {
    val packageName = this.packageName

    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
