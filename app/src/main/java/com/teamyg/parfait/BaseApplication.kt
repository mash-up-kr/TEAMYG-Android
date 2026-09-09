package com.teamyg.parfait

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.kakao.sdk.common.KakaoSdk
import com.teamyg.parfait.analytics.AnalyticsLogger
import com.teamyg.parfait.analytics.AnalyticsUserProperty
import com.teamyg.parfait.analytics.currentDeviceInfo
import com.teamyg.parfait.core.designsystem.image.newParfaitImageLoader
import com.teamyg.parfait.core.util.jvm.analytics.LoggerInitializer
import com.teamyg.parfait.push.ParfaitFirebaseMessagingService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            LoggerInitializer.setupDebug()
        }

        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        createPushNotificationChannel()
        setUpAnalytics()
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

    // debug 와 release 가 같은 GA4 속성으로 들어간다(applicationIdSuffix 가 없다).
    // 둘을 가르는 것은 IS_DEBUG 뿐이다 — parfait/adr/0031-analytics-central-screen-mapping.md.
    private fun setUpAnalytics() {
        analyticsLogger.setCollectionEnabled(true)

        val deviceInfo = currentDeviceInfo()
        with(analyticsLogger) {
            setUserProperty(AnalyticsUserProperty.IS_DEBUG, BuildConfig.ANALYTICS_IS_DEBUG.toString())
            setUserProperty(AnalyticsUserProperty.OS_TYPE, deviceInfo.osType)
            setUserProperty(AnalyticsUserProperty.OS_VER, deviceInfo.osVer)
            setUserProperty(AnalyticsUserProperty.APP_VER, deviceInfo.appVer)
            setUserProperty(AnalyticsUserProperty.APP_VER_CODE, deviceInfo.appVerCode)
            setUserProperty(AnalyticsUserProperty.DEVICE, deviceInfo.device)
            setUserProperty(AnalyticsUserProperty.APP_ID, deviceInfo.appId)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = newParfaitImageLoader(context)
}
