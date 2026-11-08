package com.teamyg.parfait.core.util.android.extension

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private const val CENTER_GUIDE_SNAP_VIBRATION_DURATION_MS = 20L
private const val CENTER_GUIDE_SNAP_VIBRATION_AMPLITUDE = 130

fun Context.triggerCenterGuideSnap() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    vibrator.vibrate(
        VibrationEffect.createOneShot(CENTER_GUIDE_SNAP_VIBRATION_DURATION_MS, CENTER_GUIDE_SNAP_VIBRATION_AMPLITUDE),
    )
}
