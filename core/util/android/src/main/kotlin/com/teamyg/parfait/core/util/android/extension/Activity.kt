package com.teamyg.parfait.core.util.android.extension

import android.app.Activity
import androidx.core.app.ActivityCompat

fun Activity.shouldShowRationale(permission: String): Boolean = ActivityCompat
    .shouldShowRequestPermissionRationale(
        this,
        permission,
    )
