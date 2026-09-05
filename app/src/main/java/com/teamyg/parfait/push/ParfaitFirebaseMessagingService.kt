package com.teamyg.parfait.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.teamyg.parfait.MainActivity
import com.teamyg.parfait.R
import com.teamyg.parfait.core.util.android.permission.NotificationPermissionManager
import com.teamyg.parfait.core.util.jvm.analytics.Loggers
import com.teamyg.parfait.domain.notification.DeviceTokenRegistrar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 모든 알림에 보내는 채널. [BaseApplication] 이 앱 시작 시 만든다. */
const val PUSH_NOTIFICATION_CHANNEL_ID = "parfait_default"

private val pushLogger = Loggers.create("Push")

/**
 * 앱이 포그라운드일 때만 이 `onMessageReceived` 를 거친다. `notification` 블록이 있는
 * payload를 앱이 백그라운드·종료 상태에서 받으면 시스템이 이 서비스를 거치지 않고 알림을
 * 자동으로 띄운다 — 그때는 `data` 의 key 가 [MainActivity] 의 Intent extras 에 그대로 실려서
 * [toPushDeepLinkOrNull][com.teamyg.parfait.pushdeeplink.toPushDeepLinkOrNull] 이 그걸 그대로
 * 읽는다. 여기서 직접 만드는 알림도 같은 key 이름으로 extras 를 채워 두 경로를 통일한다.
 *
 * FCM 페이로드 스펙 v1은 세 알림(P-01/P-02/P-03) 모두 `notification` 블록을 항상 함께
 * 보낸다 — `data` 만 있는 payload는 지금 스펙에 없다. 그래서 [onMessageReceived] 는
 * `message.notification` 이 없으면 곧장 return 하고 `data` 는 안 본다. 나중에 `data`-only
 * payload(예: 알림 없이 상태만 조용히 갱신)가 스펙에 생기면, 그 분기를 여기 추가해야 한다 —
 * 지금은 처리하지 않는다.
 */
@AndroidEntryPoint
class ParfaitFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var deviceTokenRegistrar: DeviceTokenRegistrar

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

    /**
     * 전달받은 [token] 을 그대로 쓰지 않고 등록구를 부른다 — 그쪽이 지금 값을 다시 읽는다.
     * 세션 축 등록과 같은 뮤텍스를 타야 같은 토큰이 동시에 두 번 올라가지 않는다.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        deviceTokenRegistrar.register()
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        notificationId: Int,
    ) {
        if (!NotificationPermissionManager.hasPermission(context = this)) return

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
