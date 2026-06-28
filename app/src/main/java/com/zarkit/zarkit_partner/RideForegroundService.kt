package com.zarkit.zarkit_partner

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RideForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) return START_NOT_STICKY

        val rideId = intent.getLongExtra("rideId", -1L)
        if (rideId == -1L) return START_NOT_STICKY

        val pickup = intent.getStringExtra("pickup") ?: "Pickup"
        val drop = intent.getStringExtra("drop") ?: "Drop"
        val fare = intent.getStringExtra("fare") ?: "--"

        // Silent foreground notification — system requirement
        val silentNotification = NotificationCompat.Builder(this, SILENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.zarkit_z)
            .setContentTitle("")
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()



        //startForeground(FOREGROUND_ID, silentNotification)

        // new code for andriod 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_ID,
                silentNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE  // ✅ PHONE_CALL se change karo
            )
        } else {
            startForeground(FOREGROUND_ID, silentNotification)
        }

        // Popup intent
        val popupIntent = Intent(this, RideRequestPopupActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("rideId", rideId)
            putExtra("pickup", pickup)
            putExtra("drop", drop)
            putExtra("fare", fare)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            rideId.toInt(),
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ride notification
        val rideNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.zarkit_z)
            .setContentTitle("Incoming Ride")
            .setContentText("$pickup → $drop | ₹$fare")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(false)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, rideNotification)

        // Direct popup open
        startActivity(popupIntent)

        stopSelf()

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val soundUri = android.net.Uri.parse(
                "android.resource://${packageName}/${R.raw.ride_request_tone}"
            )

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ride Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming ride requests"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
            }

            val silentChannel = NotificationChannel(
                SILENT_CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(silentChannel)
        }
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "ride_request_channel"
        const val SILENT_CHANNEL_ID = "silent_foreground_channel"
        const val NOTIFICATION_ID = 1001
        const val FOREGROUND_ID = 1002
    }
}