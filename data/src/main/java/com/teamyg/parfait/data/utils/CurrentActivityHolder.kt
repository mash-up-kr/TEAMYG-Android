package com.teamyg.parfait.data.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 resumed 상태인 Activity 를 약한 참조로 보관
 */
@Singleton
class CurrentActivityHolder
@Inject
constructor() : Application.ActivityLifecycleCallbacks {
    private var currentActivity: WeakReference<Activity>? = null

    fun current(): Activity? = currentActivity?.get()

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity?.get() === activity) {
            currentActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
