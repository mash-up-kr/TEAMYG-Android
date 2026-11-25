package com.teamyg.parfait.core.util.android.extension

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play 인앱 리뷰 플로우를 요청한다.
 */
fun Context.requestInAppReview() {
    val activity = this as? Activity ?: return
    val manager = ReviewManagerFactory.create(this)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
    }
}
