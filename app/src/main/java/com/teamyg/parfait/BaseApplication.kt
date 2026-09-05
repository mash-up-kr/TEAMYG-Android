package com.teamyg.parfait

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.kakao.sdk.common.KakaoSdk
import com.teamyg.parfait.core.designsystem.image.newParfaitImageLoader
import com.teamyg.parfait.core.util.jvm.analytics.LoggerInitializer
import com.teamyg.parfait.push.ParfaitFirebaseMessagingService
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
        createPushNotificationChannel()
    }

    // 모든 알림에 channel_id="parfait_default" 설정
    // minSdk 가 이미 26(O) 이라 버전 분기 없이 항상 만들 수 있다.
    private fun createPushNotificationChannel() {
        val channel = NotificationChannel(
            ParfaitFirebaseMessagingService.PUSH_NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_default_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_default_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = newParfaitImageLoader(context)
}
