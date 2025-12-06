package com.example.ocx_1001_driverapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ocx_1001_driverapp.RideRequestPopupActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DriverFirebaseService : FirebaseMessagingService() {

    // Called whenever Firebase gives a NEW token
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // TODO: Send this token to your Spring Boot backend
        // Example:
        // ApiClient.api.saveDriverToken(SaveTokenBody(driverId, token))

        println("🔥 NEW DRIVER FCM TOKEN = $token")
    }

    // Called when PUSH arrives
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "New Ride Request"
        val body = message.notification?.body ?: "You have a new ride request"

        // Read extra data from server (optional)
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

        // Intent → Popup screen
        val intent = Intent(this, RideRequestPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // Pass data to popup
            putExtra("fare", fare)
            putExtra("vehicle", vehicle)
            putExtra("pickup", pickup)
            putExtra("drop", drop)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "ride_request_channel"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ride Request Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Notification with full-screen intent (important!)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)   // Make it popup like call
            .setFullScreenIntent(pendingIntent, true)        // FULL SCREEN POPUP
            .setContentIntent(pendingIntent)

        notificationManager.notify(1001, builder.build())
    }
}
