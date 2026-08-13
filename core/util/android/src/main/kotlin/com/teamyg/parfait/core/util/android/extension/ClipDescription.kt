package com.teamyg.parfait.core.util.android.extension

import android.content.ClipDescription
import android.os.Build

/**
 * 비밀번호처럼 민감한 정보로 표시된 클립보드인지 확인한다.
 *
 * [ClipDescription.EXTRA_IS_SENSITIVE] 는 Android 13 부터 제공된다.
 */
fun ClipDescription.isSensitive(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return false
    }

    return extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
}
