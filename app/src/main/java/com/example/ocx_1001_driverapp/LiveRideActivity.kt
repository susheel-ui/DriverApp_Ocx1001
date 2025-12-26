package com.example.ocx_1001_driverapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LiveRideActivity : AppCompatActivity() {

    private var rideId: Long = -1L

    // Dummy values (replace from API response)
    private var pickupLat = 0.0
    private var pickupLng = 0.0
    private var dropLat = 0.0
    private var dropLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_ride)

        // 🔒 Disable back
        onBackPressedDispatcher.addCallback(this) { }

        rideId = intent.getLongExtra("rideId", -1L)

        if (rideId == -1L) {
            Toast.makeText(this, "Invalid ride", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val txtInfo = findViewById<TextView>(R.id.txtRideInfo)
        val btnNavigatePickup = findViewById<Button>(R.id.btnNavigatePickup)
        val btnStartTrip = findViewById<Button>(R.id.btnStartTrip)
        val btnNavigateDrop = findViewById<Button>(R.id.btnNavigateDrop)
        val btnEndTrip = findViewById<Button>(R.id.btnEndTrip)

        btnNavigateDrop.visibility = View.GONE
        btnEndTrip.visibility = View.GONE

        // 🔥 Fetch ride details
        fetchRideDetails(txtInfo)

        // ---------------- NAVIGATE PICKUP ----------------
        btnNavigatePickup.setOnClickListener {
            openGoogleMaps(pickupLat, pickupLng)
        }

        // ---------------- START TRIP ----------------
        btnStartTrip.setOnClickListener {
            startTrip {
                btnNavigatePickup.visibility = View.GONE
                btnStartTrip.visibility = View.GONE
                btnNavigateDrop.visibility = View.VISIBLE
                btnEndTrip.visibility = View.VISIBLE
            }
        }

        // ---------------- NAVIGATE DROP ----------------
        btnNavigateDrop.setOnClickListener {
            openGoogleMaps(dropLat, dropLng)
        }

        // ---------------- END TRIP ----------------
        btnEndTrip.setOnClickListener {
            endTrip()
        }
    }

    // ==================================================
    // API CALLS
    // ==================================================

    private fun fetchRideDetails(txtInfo: TextView) {
        // 🔥 Replace with real API
        // ApiClient.api.getRideDetails(rideId)

        // Dummy data for now
        pickupLat = 28.6139
        pickupLng = 77.2090
        dropLat = 28.5355
        dropLng = 77.3910

        txtInfo.text = "Pickup → Drop\nRide ID: $rideId"
    }

    private fun startTrip(onSuccess: () -> Unit) {
        ApiClient.api.startTrip(rideId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@LiveRideActivity, "Trip started", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        Toast.makeText(this@LiveRideActivity, "Cannot start trip", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@LiveRideActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun endTrip() {
        ApiClient.api.endTrip(rideId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    LocalStorage.clearActiveRideId(this@LiveRideActivity)
                    Toast.makeText(this@LiveRideActivity, "Trip completed", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@LiveRideActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ==================================================
    // GOOGLE MAPS
    // ==================================================

    private fun openGoogleMaps(lat: Double, lng: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }
}
