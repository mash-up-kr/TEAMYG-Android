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
    companion object {
        const val CHANNEL_ID = "fcm_default_channel"
    }

    override fun onRegistered(token: String) {
        super.onRegistered(token)
        Log.d(TAG, "FCM Token: $token")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            val notification = NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(remoteMessage.notification?.title)
                .setContentText(remoteMessage.notification?.body)
                .build()

            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
