package com.example.ocx_1001_driverapp

import android.content.Intent
import android.os.Build
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

        val data = message.data

        val rideId = data["rideId"]?.toLongOrNull() ?: return
        val pickup = data["pickup"]
        val drop = data["drop"]
        val fare = data["fare"]

        // 🔥 ALWAYS START FOREGROUND SERVICE
        val serviceIntent = Intent(this, RideForegroundService::class.java).apply {
            putExtra("rideId", rideId)
            putExtra("pickup", pickup)
            putExtra("drop", drop)
            putExtra("fare", fare)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
