package com.teamyg.parfait.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.teamyg.parfait.MainActivity
import com.teamyg.parfait.R
import com.teamyg.parfait.core.util.jvm.analytics.Loggers

/** 모든 알림에 보내는 채널. [BaseApplication] 이 앱 시작 시 만든다. */
const val PUSH_NOTIFICATION_CHANNEL_ID = "parfait_default"

private val pushLogger = Loggers.create("Push")

/**
 * 앱이 포그라운드일 때, 또는 `data` 만 있고 `notification` 이 없는 payload 일 때만 이
 * `onMessageReceived` 를 거친다. `notification` 블록이 있는 payload를 앱이 백그라운드·종료
 * 상태에서 받으면 시스템이 이 서비스를 거치지 않고 알림을 자동으로 띄운다 — 그때는
 * `data` 의 key 가 [MainActivity] 의 Intent extras 에 그대로 실려서
 * [toPushDeepLinkOrNull][com.teamyg.parfait.pushdeeplink.toPushDeepLinkOrNull] 이 그걸 그대로
 * 읽는다. 여기서 직접 만드는 알림도 같은 key 이름으로 extras 를 채워 두 경로를 통일한다.
 */
class ParfaitFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification ?: return
        showNotification(
            title = notification.title.orEmpty(),
            body = notification.body.orEmpty(),
            data = message.data,
            notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(),
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: 디바이스 토큰 등록 API 스펙이 아직 배포되지 않았다. 확정되면 여기서 서버에 등록한다.
        pushLogger.i { "새 FCM 토큰을 받았지만 등록 API가 아직 없어 아무 것도 하지 않는다" }
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        notificationId: Int,
    ) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(this, PUSH_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }
}
