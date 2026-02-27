package com.zarkit.zarkit_partner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.CallRideRequest
import com.zarkit.zarkit_partner.api.RideDetailsResponse
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
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class LiveRideActivity : AppCompatActivity(), OnMapReadyCallback {

    private var rideId: Long = -1L
    private lateinit var authHeader: String

    private lateinit var googleMap: GoogleMap
    private var isMapReady = false

    private var pickupLat = 0.0
    private var pickupLng = 0.0
    private var dropLat = 0.0
    private var dropLng = 0.0

    private var pickupAddress: String? = null
    private var dropAddress: String? = null

    private lateinit var txtInfo: TextView
    private lateinit var btnCallUser: ImageButton
    private lateinit var btnNavigatePickup: Button
    private lateinit var btnNavigateDrop: Button

    // 🔥 Back button
    private lateinit var btnBack: ImageView

    // 🔥 Drag buttons
    private lateinit var dragStartTrip: View
    private lateinit var sliderStartTrip: View
    private lateinit var dragEndTrip: View
    private lateinit var sliderEndTrip: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_live_ride)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔒 Disable default back (we handle it)
        onBackPressedDispatcher.addCallback(this) {
            goToDashboard()
        }

        // 🔙 BACK BUTTON
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            goToDashboard()
        }

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
        btnNavigateDrop = findViewById(R.id.btnNavigateDrop)

        dragStartTrip = findViewById(R.id.dragStartTrip)
        sliderStartTrip = findViewById(R.id.sliderStartTrip)
        dragEndTrip = findViewById(R.id.dragEndTrip)
        sliderEndTrip = findViewById(R.id.sliderEndTrip)

        // 🔥 Drag logic
        setupDragButton(sliderStartTrip, dragStartTrip) {
            startTrip()
        }

        setupDragButton(sliderEndTrip, dragEndTrip) {
            endTripAndGoToCollection()
        }

        resetButtons()
        fetchRideDetails()

        // CALL USER
        btnCallUser.setOnClickListener {
            callUserViaBackend()
        }

        btnNavigatePickup.setOnClickListener {
            openGoogleMaps(pickupLat, pickupLng)
        }

        btnNavigateDrop.setOnClickListener {
            openGoogleMaps(dropLat, dropLng)
        }
    }

    // ================= BACK NAVIGATION =================

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    // ================= DRAG BUTTON =================

    private fun setupDragButton(
        slider: View,
        parent: View,
        onComplete: () -> Unit
    ) {
        var startX = 0f

        slider.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX - v.x
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX - startX
                    val maxX = parent.width - v.width
                    v.x = newX.coerceIn(0f, maxX.toFloat())
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (v.x > parent.width * 0.7f) {
                        v.animate()
                            .x((parent.width - v.width).toFloat())
                            .setDuration(150)
                            .start()
                        onComplete()
                    } else {
                        v.animate().x(0f).setDuration(150).start()
                    }
                    true
                }

                else -> false
            }
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
                    val ride = response.body() ?: return

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
                    toast("Network error")
                }
            })
    }

    private fun startTrip() {
        ApiClient.api.startTrip(authHeader, rideId)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) fetchRideDetails()
                    else toast("Failed to start trip")
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    private fun endTripAndGoToCollection() {

        ApiClient.api.endTrip(authHeader, rideId)
            .enqueue(object : Callback<Map<String, Any>> {

                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {

                        // 🔥 STOP LIVE LOCATION SERVICE IMMEDIATELY
                        val locationIntent = Intent(this@LiveRideActivity, DriverLocationService::class.java)
                        stopService(locationIntent)

                        // 🔥 CLEAR ACTIVE RIDE ID
                        LocalStorage.clearActiveRideId(this@LiveRideActivity)


                        // 🔥 CHECK PAYMENT HERE
                        ApiClient.api.checkPaymentStatus(authHeader, rideId)
                            .enqueue(object : Callback<Boolean> {

                                override fun onResponse(
                                    call: Call<Boolean>,
                                    paymentResponse: Response<Boolean>
                                ) {

                                    val isPaid = paymentResponse.body() ?: false

                                    val intent = Intent(
                                        this@LiveRideActivity,
                                        CollectPaymentActivity::class.java
                                    )

                                    intent.putExtra("rideId", rideId)
                                    intent.putExtra("isPaid", isPaid)

                                    startActivity(intent)
                                    finish()
                                }

                                override fun onFailure(call: Call<Boolean>, t: Throwable) {
                                    toast("Failed to check payment")
                                }
                            })

                    } else {
                        toast("Failed to end trip")
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    toast("Network error")
                }
            })
    }


    // ================= UI =================

    private fun resetButtons() {
        btnNavigatePickup.visibility = View.GONE
        btnNavigateDrop.visibility = View.GONE
        dragStartTrip.visibility = View.GONE
        dragEndTrip.visibility = View.GONE
    }

    private fun updateUIByStatus(status: String) {
        resetButtons()
        when (status) {
            "ACCEPTED" -> {
                btnNavigatePickup.visibility = View.VISIBLE
                dragStartTrip.visibility = View.VISIBLE
                showPickupOnMap()
            }

            "IN_PROGRESS", "STARTED" -> {
                btnNavigateDrop.visibility = View.VISIBLE
                dragEndTrip.visibility = View.VISIBLE
                showDropOnMap()
            }
        }
    }

    // ================= UTILS =================

    private fun getAddressFromLatLng(lat: Double, lng: Double): String? =
        try {
            Geocoder(this, Locale.getDefault())
                .getFromLocation(lat, lng, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        } catch (e: Exception) {
            null
        }

    private fun openGoogleMaps(lat: Double, lng: Double) {

        if (lat == 0.0 || lng == 0.0) {
            toast("Location not available")
            return
        }

        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            val webUri =
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun callUserViaBackend() {
        ApiClient.api.callRideConnect(authHeader, CallRideRequest(rideId))
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    toast(response.body() ?: "Calling user…")
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
