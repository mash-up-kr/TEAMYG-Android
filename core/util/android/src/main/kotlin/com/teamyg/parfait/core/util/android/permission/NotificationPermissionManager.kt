package com.teamyg.parfait.core.util.android.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import com.teamyg.parfait.core.util.android.extension.isGrantedPermission
import com.teamyg.parfait.core.util.android.extension.shouldShowRationale

/**
 * `POST_NOTIFICATIONS` 는 API 33 에 생긴 권한이라 그 아래 플랫폼에는 아예 정의돼 있지 않다.
 * 정의되지 않은 권한은 `checkSelfPermission` 이 항상 거부로 답하고 요청해도 시스템
 * 다이얼로그 없이 즉시 거부 콜백이 오는데, 정작 알림 자체는 OS 설정에서 기본으로 켜져
 * 있다 — 버전으로 먼저 가르지 않으면 판정이 사실과 정반대가 된다.
 *
 * `sdkInt` 를 인자로 받는 이유는 이 갈림을 기기 없이 테스트하기 위해서다.
 */
object NotificationPermissionManager {
    fun isRuntimePermissionRequired(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU

    fun hasPermission(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = if (isRuntimePermissionRequired(sdkInt)) {
        context.isGrantedPermission(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        true
    }

    /**
     * **요청 콜백 안에서만 의미가 있다.** 첫 요청 전에도 `false` 라서 그 밖에서 부르면
     * "아직 안 물어봤다"와 "영구 거부"를 구분하지 못한다. 거부로 돌아온 직후라면 `false`
     * 는 영구 거부뿐이다.
     */
    fun isPermanentlyDenied(activity: Activity): Boolean =
        !activity.shouldShowRationale(permission = Manifest.permission.POST_NOTIFICATIONS)
}
