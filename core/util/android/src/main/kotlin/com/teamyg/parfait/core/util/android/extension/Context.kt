package com.teamyg.parfait.core.util.android.extension

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

fun Context.buildAppSettingsIntent(): Intent {
    val packageName = this.packageName

    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

fun Context.buildAppNotificationSettingsIntent(): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

fun Context.isGrantedPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * [link] 를 `ACTION_VIEW` 로 연다. 받을 앱이 없거나 권한으로 막히면 예외 대신 `false` 를 돌려줘
 * 호출부가 다음 후보로 넘어갈 수 있게 한다.
 *
 * @param packageName 지정하면 그 앱으로만 연다. `null` 이면 시스템이 고른다
 */
fun Context.tryStartViewIntent(
    link: String,
    packageName: String? = null,
): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, link.toUri()).setPackage(packageName))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

/** 텍스트를 시스템 공유 시트로 넘긴다. */
fun Context.shareText(text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(sendIntent, null))
}
