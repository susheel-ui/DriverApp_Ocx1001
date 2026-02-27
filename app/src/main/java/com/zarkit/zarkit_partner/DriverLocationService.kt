package com.zarkit.zarkit_partner

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.zarkit.zarkit_partner.api.ApiClient
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        startForeground(101, createNotification())

        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        startLocationUpdates()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location = result.lastLocation ?: return

                ApiClient.api.sendDriverLocation(
                    "Bearer ${LocalStorage.getToken(this@DriverLocationService)}",
                    mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude
                    )
                ).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {}
                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                })
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onDestroy() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        stopForeground(true)
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        // Image for BigPictureStyle
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.zarkit_nobgyellow)

        // Intent to open your app on notification click
        val intent = Intent(this, LiveRideActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("rideId", LocalStorage.getActiveRideId(this))

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "driver_location_channel")
            .setContentTitle("Ride in progress")
            .setContentText("Sharing live location")
            .setSmallIcon(R.drawable.zarkit_nobgyellow)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?) // explicit cast fixes ambiguity
            )
            .setContentIntent(pendingIntent) // open app on click
            .setOngoing(false) // now swipeable
            .setAutoCancel(true) // dismiss when clicked
            .build()
    }


}
