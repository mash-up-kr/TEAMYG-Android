package com.teamyg

import android.app.Application
import com.teamyg.analytics.utils.TimberUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        TimberUtils.setPlant(isDebug = BuildConfig.DEBUG)
    }
}
