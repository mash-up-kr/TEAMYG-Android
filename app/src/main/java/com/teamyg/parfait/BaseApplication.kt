package com.teamyg.parfait

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.kakao.sdk.common.KakaoSdk
import com.teamyg.parfait.core.designsystem.image.newParfaitImageLoader
import com.teamyg.parfait.core.util.jvm.analytics.LoggerInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            LoggerInitializer.setupDebug()
        }

        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }

    // Coil 이 싱글턴 로더를 처음 만들 때 여기를 묻는다. 설정은 designsystem 한곳에 있다
    override fun newImageLoader(context: PlatformContext): ImageLoader = newParfaitImageLoader(context)
}
