package com.example.ocx_1001_driverapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.CallRideRequest
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
import android.location.Geocoder
import java.util.Locale

class LiveRideActivity : AppCompatActivity(), OnMapReadyCallback {

    private var rideId: Long = -1L
    private lateinit var authHeader: String

    private lateinit var googleMap: GoogleMap
    private var pickupAddress: String? = null
    private var dropAddress: String? = null
    private var isMapReady = false

    private var pickupLat = 0.0
    private var pickupLng = 0.0
    private var dropLat = 0.0
    private var dropLng = 0.0

    private lateinit var txtInfo: TextView
    private lateinit var btnCallUser: Button
    private lateinit var btnNavigatePickup: Button
    private lateinit var btnStartTrip: Button
    private lateinit var btnNavigateDrop: Button
    private lateinit var btnEndTrip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_ride)

        onBackPressedDispatcher.addCallback(this) {}

        // MAP
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // RIDE ID
        rideId = intent.getLongExtra("rideId", -1L)
        if (rideId <= 0) {
            toast("Invalid ride")
            finish()
            return
        }

        // TOKEN
        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            toast("Session expired")
            finish()
            return
        }
        authHeader = "Bearer $token"

        // VIEWS
        txtInfo = findViewById(R.id.txtRideInfo)
        btnCallUser = findViewById(R.id.btnCallUser)
        btnNavigatePickup = findViewById(R.id.btnNavigatePickup)
        btnStartTrip = findViewById(R.id.btnStartTrip)
        btnNavigateDrop = findViewById(R.id.btnNavigateDrop)
        btnEndTrip = findViewById(R.id.btnEndTrip)

        resetButtons()
        fetchRideDetails()

        // CALL USER
        btnCallUser.setOnClickListener {
            callUserViaBackend()
        }

        btnNavigatePickup.setOnClickListener {
            openGoogleMaps(pickupAddress, pickupLat, pickupLng)
        }

        btnNavigateDrop.setOnClickListener {
            openGoogleMaps(dropAddress, dropLat, dropLng)
        }


        btnStartTrip.setOnClickListener {
            startTrip()
        }


        btnEndTrip.setOnClickListener {

            endTripAndGoToCollection()
        }
    }

    // ================= MAP =================

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun showPickupOnMap() {
        if (!isMapReady) return
        val pickup = LatLng(pickupLat, pickupLng)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(pickup).title("Pickup"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, 15f))
    }

    private fun endTripAndGoToCollection() {

        ApiClient.api.endTrip(authHeader, rideId)
            .enqueue(object : Callback<Map<String, Any>> {

                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {

                        // ✅ Trip ended successfully
                        val intent = Intent(
                            this@LiveRideActivity,
                            CollectPaymentActivity::class.java
                        )
                        intent.putExtra("rideId", rideId) // ✅ ONLY rideId
                        startActivity(intent)
                        finish()

                    } else {
                        toast("Failed to end trip")
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    toast("Network error")
                }
            })
    }


    private fun getAddressFromLatLng(lat: Double, lng: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val list = geocoder.getFromLocation(lat, lng, 1)
            if (!list.isNullOrEmpty()) {
                list[0].getAddressLine(0)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun showDropOnMap() {
        if (!isMapReady) return
        val drop = LatLng(dropLat, dropLng)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(drop).title("Drop"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(drop, 15f))
    }

    // ================= API =================

    private fun fetchRideDetails() {
        ApiClient.api.getRideDetails(authHeader, rideId)
            .enqueue(object : Callback<RideDetailsResponse> {

                override fun onResponse(
                    call: Call<RideDetailsResponse>,
                    response: Response<RideDetailsResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        toast("Failed to load ride (${response.code()})")
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

                    pickupAddress = getAddressFromLatLng(pickupLat, pickupLng)
                    dropAddress = getAddressFromLatLng(dropLat, dropLng)

                    updateUIByStatus(ride.status)

                }

                override fun onFailure(call: Call<RideDetailsResponse>, t: Throwable) {
                    t.printStackTrace()
                    toast(t.message ?: "Network error")
                }
            })
    }

    // ================= CALL USER =================

// ================= CALL USER =================

    private fun callUserViaBackend() {

        val request = CallRideRequest(rideId)

        ApiClient.api.callRideConnect(authHeader, request)
            .enqueue(object : Callback<String> {

                override fun onResponse(
                    call: Call<String>,
                    response: Response<String>
                ) {
                    if (response.isSuccessful) {
                        toast(response.body() ?: "Calling user…")
                    } else {
                        toast("Call failed (${response.code()})")
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.e("CALL_API", "Error", t)
                    toast(t.message ?: "Network error")
                }
            })
    }





    // ================= TRIP FLOW =================

    private fun startTrip() {
        ApiClient.api.startTrip(authHeader, rideId)
            .enqueue(object : Callback<Map<String, Any>> {

                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {
                        toast("Trip started")
                        fetchRideDetails()
                    } else {
                        toast("Failed to start trip")
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    t.printStackTrace()
                    toast(t.message ?: "Network error")
                }
            })
    }


    // ================= UI =================

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
                showPickupOnMap()
            }

            "IN_PROGRESS", "STARTED" -> {
                btnNavigateDrop.visibility = View.VISIBLE
                btnEndTrip.visibility = View.VISIBLE
                showDropOnMap()
            }
        }
    }


    // ================= UTILS =================

    private fun openGoogleMaps(address: String?, lat: Double, lng: Double) {

        if (lat == 0.0 || lng == 0.0) {
            toast("Location not available")
            return
        }

        val destination = address ?: "$lat,$lng"

        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            toast("Google Maps not installed")
        }
    }


    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
