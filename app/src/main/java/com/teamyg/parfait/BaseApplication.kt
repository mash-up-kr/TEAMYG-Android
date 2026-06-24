package com.teamyg.parfait

import android.app.Application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Logger.setLogWriters(listOf(platformLogWriter()))
        }

        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
