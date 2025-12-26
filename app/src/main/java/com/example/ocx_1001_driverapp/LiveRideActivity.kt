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
import com.example.ocx_1001_driverapp.api.RideDetailsResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LiveRideActivity : AppCompatActivity(), OnMapReadyCallback {

    private var rideId: Long = -1L
    private lateinit var authHeader: String

    private lateinit var googleMap: GoogleMap
    private var isMapReady = false   // ✅ ADDED

    private var pickupLat = 0.0
    private var pickupLng = 0.0
    private var dropLat = 0.0
    private var dropLng = 0.0

    private lateinit var txtInfo: TextView
    private lateinit var btnNavigatePickup: Button
    private lateinit var btnStartTrip: Button
    private lateinit var btnNavigateDrop: Button
    private lateinit var btnEndTrip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_ride)

        onBackPressedDispatcher.addCallback(this) {}

        // ================= MAP INIT =================
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // ================= RIDE ID =================
        rideId = intent.getLongExtra("rideId", -1L)
        if (rideId == -1L) {
            toast("Invalid ride")
            finish()
            return
        }

        // ================= TOKEN =================
        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            toast("Session expired")
            finish()
            return
        }
        authHeader = "Bearer $token"

        // ================= VIEWS =================
        txtInfo = findViewById(R.id.txtRideInfo)
        btnNavigatePickup = findViewById(R.id.btnNavigatePickup)
        btnStartTrip = findViewById(R.id.btnStartTrip)
        btnNavigateDrop = findViewById(R.id.btnNavigateDrop)
        btnEndTrip = findViewById(R.id.btnEndTrip)

        resetButtons()
        fetchRideDetails()

        btnNavigatePickup.setOnClickListener {
            openGoogleMaps(pickupLat, pickupLng)
        }

        btnStartTrip.setOnClickListener {
            startTrip()
        }

        btnNavigateDrop.setOnClickListener {
            openGoogleMaps(dropLat, dropLng)
        }

        btnEndTrip.setOnClickListener {
            endTrip()
        }
    }

    // ==================================================
    // MAP READY
    // ==================================================

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    // ==================================================
    // API
    // ==================================================

    private fun fetchRideDetails() {
        ApiClient.api.getRideDetails(authHeader, rideId)
            .enqueue(object : Callback<RideDetailsResponse> {

                override fun onResponse(
                    call: Call<RideDetailsResponse>,
                    response: Response<RideDetailsResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        toast("Failed to load ride")
                        return
                    }

                    val ride = response.body()!!

                    pickupLat = ride.pickupLat
                    pickupLng = ride.pickupLon
                    dropLat = ride.dropLat
                    dropLng = ride.dropLon

                    txtInfo.text = """
                        Ride ID: ${ride.rideId}
                        Fare: ₹${ride.finalFare}
                        Status: ${ride.status}
                    """.trimIndent()

                    updateUIByStatus(ride.status)
                    showPickupOnMap()
                }

                override fun onFailure(call: Call<RideDetailsResponse>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    // ==================================================
    // MAP HELPERS
    // ==================================================

    private fun showPickupOnMap() {
        if (!isMapReady) return

        val pickup = LatLng(pickupLat, pickupLng)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(pickup).title("Pickup"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, 15f))
    }

    private fun showDropOnMap() {
        if (!isMapReady) return

        val drop = LatLng(dropLat, dropLng)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(drop).title("Drop"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(drop, 15f))
    }

    // ==================================================
    // TRIP FLOW
    // ==================================================

    private fun startTrip() {
        ApiClient.api.startTrip(authHeader, rideId)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {
                        toast("Trip started")
                        showDropOnMap()
                        fetchRideDetails()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    private fun endTrip() {
        ApiClient.api.endTrip(authHeader, rideId)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    toast("Trip completed")
                    finish()
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    // ==================================================
    // UI
    // ==================================================

    private fun resetButtons() {
        btnNavigatePickup.visibility = View.GONE
        btnStartTrip.visibility = View.GONE
        btnNavigateDrop.visibility = View.GONE
        btnEndTrip.visibility = View.GONE
    }

    private fun updateUIByStatus(status: String) {
        resetButtons()
        when (status) {
            "ACCEPTED" -> {
                btnNavigatePickup.visibility = View.VISIBLE
                btnStartTrip.visibility = View.VISIBLE
            }
            "STARTED" -> {
                btnNavigateDrop.visibility = View.VISIBLE
                btnEndTrip.visibility = View.VISIBLE
            }
        }
    }

    // ==================================================
    // UTILS
    // ==================================================

    private fun openGoogleMaps(lat: Double, lng: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
