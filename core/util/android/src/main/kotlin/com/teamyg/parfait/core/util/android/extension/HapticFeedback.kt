package com.teamyg.parfait.core.util.android.extension

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private const val SNAP_HAPTIC_DURATION_MS = 20L
private const val SNAP_HAPTIC_AMPLITUDE = 130

/**
 * 토핑이 중앙·각도 가이드에 스냅될 때 울리는 진동 한 번. Compose `performHapticFeedback` 은 시스템
 * "터치 피드백" 설정이 꺼져 있으면 무음이라, 그 설정과 무관하게 울리도록 [Vibrator] 를 직접 쓴다.
 */
fun Context.triggerSnapHaptic() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    vibrator?.vibrate(
        VibrationEffect.createOneShot(SNAP_HAPTIC_DURATION_MS, SNAP_HAPTIC_AMPLITUDE),
    )
}
