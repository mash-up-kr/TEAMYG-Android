package com.teamyg.parfait

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.teamyg.parfait.core.util.jvm.analytics.LoggerInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            LoggerInitializer.setupDebug()
        }

        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
