package com.teamyg.parfait.fcm

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.teamyg.parfait.R

private const val TAG = "YGFirebaseMessagingService"
class YGFirebaseMessagingService : FirebaseMessagingService() {

    override fun onRegistered(token: String) {
        super.onRegistered(token)
        Log.d(TAG, "FCM Token: $token")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notification = NotificationCompat.Builder(this, "fcm_default_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(remoteMessage.notification?.title)
            .setContentText(remoteMessage.notification?.body)
            .build()

        NotificationManagerCompat.from(this).notify(0, notification)
    }
}
