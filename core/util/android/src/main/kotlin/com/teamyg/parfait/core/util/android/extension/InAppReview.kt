package com.teamyg.parfait.core.util.android.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.teamyg.parfait.core.util.android.coreUtilAndroidLogger
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play 인앱 리뷰 플로우를 요청한다. 호출 시점엔 이미 "다음 4개월 잠금"이 걸린 뒤라,
 * 여기서 조용히 실패하면 요청은 안 떴는데 잠금만 남는다 — 그래서 [Activity] 를 못 찾거나
 * Play Core 호출이 실패하면 로그를 남긴다.
 *
 * Compose 의 `LocalContext.current` 는 보통 [Activity] 를 감싼 [ContextWrapper] 라 벗겨내야
 * [Activity] 를 얻는다.
 */
fun Context.requestInAppReview() {
    val activity = findActivityOrNull()
    if (activity == null) {
        coreUtilAndroidLogger.w { "인앱 리뷰 요청 실패 - Context 에서 Activity 를 찾지 못했다" }
        return
    }

    val manager = ReviewManagerFactory.create(applicationContext)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            manager.launchReviewFlow(activity, task.result)
        } else {
            coreUtilAndroidLogger.w(task.exception) { "인앱 리뷰 요청 실패 - requestReviewFlow" }
        }
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
