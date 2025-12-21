package com.example.ocx_1001_driverapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DriverFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        LocalStorage.saveFcmToken(this, token)
        println("🔥 NEW FCM TOKEN = $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "Incoming Ride Request"
        val body = message.notification?.body ?: "Tap to view ride details"

        val fare = message.data["fare"]
        val vehicle = message.data["vehicle"]
        val pickup = message.data["pickup"]
        val drop = message.data["drop"]

        showNotification(title, body, fare, vehicle, pickup, drop)
    }

    private fun showNotification(
        title: String,
        body: String,
        fare: String?,
        vehicle: String?,
        pickup: String?,
        drop: String?
    ) {

        // OPEN POPUP DIRECTLY ON LOCKSCREEN
        val intent = Intent(this, RideRequestPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fare", fare)
            putExtra("pickup", pickup)
            putExtra("drop", drop)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 200, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // UPDATED CHANNEL ID
        val channelId = "ride_channel_v5"

        // UPDATED CUSTOM SOUND (NO DEFAULT ALARM)
        val alarmSound: Uri =
            Uri.parse("android.resource://${packageName}/raw/ride_request_tone")

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Incoming Ride Request",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 500, 700)
                setSound(alarmSound, Notification.AUDIO_ATTRIBUTES_DEFAULT)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }

        // FULL SCREEN NOTIFICATION (Porter style)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)   // IMPORTANT
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 700, 500, 700))
            .setAutoCancel(true)
            .setOngoing(true)

        nm.notify(1002, builder.build())
    }
}
