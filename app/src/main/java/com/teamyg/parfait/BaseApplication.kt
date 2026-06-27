package com.teamyg.parfait

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.teamyg.parfait.data.utils.CurrentActivityHolder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application() {
    @Inject
    lateinit var currentActivityHolder: CurrentActivityHolder

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        registerActivityLifecycleCallbacks(currentActivityHolder)
    }
}
