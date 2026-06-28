package com.zarkit.zarkit_partner

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.CallRideRequest
import com.zarkit.zarkit_partner.api.Entites.EndRideRequest
import com.zarkit.zarkit_partner.api.RideDetailsResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LiveRideActivity : BaseActivity(), OnMapReadyCallback {

    private var rideId: Long = -1L
    private lateinit var authHeader: String

    private lateinit var btnCallPickupPerson: Button
    private lateinit var btnCallDropPerson: Button

    private var callSenderTimer: CountDownTimer? = null
    private var callReceiverTimer: CountDownTimer? = null
    private var callUserTimer: CountDownTimer? = null

    private lateinit var googleMap: GoogleMap
    private var isMapReady = false

    private var pickupLat = 0.0
    private var pickupLng = 0.0
    private var dropLat = 0.0
    private var dropLng = 0.0

    private lateinit var txtInfo: TextView
    private lateinit var btnCallUser: ImageButton
    private lateinit var btnNavigatePickup: Button
    private lateinit var btnNavigateDrop: Button
    private lateinit var btnBack: ImageView

    private lateinit var dragStartTrip: View
    private lateinit var sliderStartTrip: View
    private lateinit var dragEndTrip: View
    private lateinit var sliderEndTrip: View

    private lateinit var callSection: LinearLayout
    private lateinit var txtCallUserTimer: TextView

    // ================= LOCATION =================
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationHandler = Handler(Looper.getMainLooper())
    private var isLocationRunning = false

    private val locationRunnable = object : Runnable {
        override fun run() {
            sendCurrentLocation()
            locationHandler.postDelayed(this, 5000)
        }
    }

    // ================= LIFECYCLE =================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_live_ride)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this) {
            goToDashboard()
        }

        // Init fused location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { goToDashboard() }

        btnCallPickupPerson = findViewById(R.id.btnCallPickupPerson)
        btnCallDropPerson = findViewById(R.id.btnCallDropPerson)
        btnCallPickupPerson.setOnClickListener { callSender() }
        btnCallDropPerson.setOnClickListener { callReceiver() }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        rideId = intent.getLongExtra("rideId", -1L)
        if (rideId <= 0) {
            toast("Invalid ride")
            finish()
            return
        }

        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            toast("Session expired")
            finish()
            return
        }

        authHeader = "Bearer $token"

        txtInfo           = findViewById(R.id.txtRideInfo)
        btnCallUser       = findViewById(R.id.btnCallUser)
        btnNavigatePickup = findViewById(R.id.btnNavigatePickup)
        btnNavigateDrop   = findViewById(R.id.btnNavigateDrop)
        txtCallUserTimer  = findViewById(R.id.txtCallUserTimer)
        dragStartTrip     = findViewById(R.id.dragStartTrip)
        sliderStartTrip   = findViewById(R.id.sliderStartTrip)
        dragEndTrip       = findViewById(R.id.dragEndTrip)
        sliderEndTrip     = findViewById(R.id.sliderEndTrip)
        callSection       = findViewById(R.id.callSection)

        setupDragButton(sliderStartTrip, dragStartTrip) { startTrip() }
        setupDragButton(sliderEndTrip, dragEndTrip) { showEndTripOtpDialog() }

        resetButtons()
        fetchRideDetails()

        btnCallUser.setOnClickListener { callUserViaBackend() }
        btnNavigatePickup.setOnClickListener { openGoogleMaps(pickupLat, pickupLng) }
        btnNavigateDrop.setOnClickListener { openGoogleMaps(dropLat, dropLng) }
    }

    // ================= LOCATION =================

    @SuppressLint("MissingPermission")
    private fun sendCurrentLocation() {
        android.util.Log.d("LOC", "sendCurrentLocation() called")

        val locationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
            .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(10000)
            .build()

        fusedLocationClient.getCurrentLocation(locationRequest, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    android.util.Log.e("LOC", "getCurrentLocation also NULL")
                    return@addOnSuccessListener
                }

                android.util.Log.d("LOC", "Got location: ${location.latitude}, ${location.longitude}")

                val token = LocalStorage.getToken(this) ?: run {
                    android.util.Log.e("LOC", "Token is NULL")
                    return@addOnSuccessListener
                }

                ApiClient.api.sendDriverLocation(
                    "Bearer $token",
                    mapOf("latitude" to location.latitude, "longitude" to location.longitude)
                ).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        android.util.Log.d("LOC", "API Response: ${response.code()}")
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        android.util.Log.e("LOC", "API FAILED: ${t.message}")
                    }
                })
            }
            .addOnFailureListener { e ->
                android.util.Log.e("LOC", "getCurrentLocation FAILED: ${e.message}")
            }
    }
    private fun startLocationUpdates() {
        if (isLocationRunning) return
        isLocationRunning = true
        locationHandler.post(locationRunnable)
        android.util.Log.d("LOC", "Location updates started")
    }

    private fun stopLocationUpdates() {
        isLocationRunning = false
        locationHandler.removeCallbacks(locationRunnable)
        android.util.Log.d("LOC", "Location updates stopped")
    }

    // ================= CALL =================

    private fun callSender() {
        val currentRideId = rideId.takeIf { it > 0 }
            ?: LocalStorage.getActiveRideId(this).takeIf { it > 0 }
            ?: run { toast("Ride ID नहीं मिला"); return }

        ApiClient.api.callSender(authHeader, CallRideRequest(currentRideId))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    toast("Please wait, we will connect your call\nकॉल कनेक्ट हो रही है, कृपया प्रतीक्षा करें...")
                    startCallSenderTimer()
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    private fun callReceiver() {
        val currentRideId = rideId.takeIf { it > 0 }
            ?: LocalStorage.getActiveRideId(this).takeIf { it > 0 }
            ?: run { toast("Ride ID नहीं मिला"); return }

        ApiClient.api.callReceiver(authHeader, CallRideRequest(currentRideId))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    toast("Please wait, we will connect your call\nकॉल कनेक्ट हो रही है, कृपया प्रतीक्षा करें...")
                    startCallReceiverTimer()
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    // ================= DRAG BUTTON =================

    private fun setupDragButton(slider: View, parent: View, onComplete: () -> Unit) {
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
                        v.animate().x((parent.width - v.width).toFloat()).setDuration(150).start()
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

    // ================= OTP DIALOG =================

    private fun showEndTripOtpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_end_trip_otp, null)

        val edtOtp   = dialogView.findViewById<EditText>(R.id.edtEndTripOtp)
        val txtError = dialogView.findViewById<TextView>(R.id.txtOtpError)
        val btnVerify = dialogView.findViewById<Button>(R.id.btnVerifyEndTripOtp)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnVerify.setOnClickListener {
            val otp = edtOtp.text.toString().trim()
            if (otp.length != 4) {
                txtError.text = "Please enter 4 digit OTP"
                txtError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            txtError.visibility = View.GONE
            btnVerify.isEnabled = false
            btnVerify.text = "Verifying..."

            endTripAndGoToCollection(otp = otp, dialog = dialog, txtError = txtError, btnVerify = btnVerify)
        }

        dialog.setOnDismissListener {
            sliderEndTrip.animate().x(0f).setDuration(150).start()
        }

        dialog.show()
    }

    // ================= API CALLS =================

    private fun fetchRideDetails() {
        ApiClient.api.getRideDetails(authHeader, rideId)
            .enqueue(object : Callback<RideDetailsResponse> {
                override fun onResponse(call: Call<RideDetailsResponse>, response: Response<RideDetailsResponse>) {
                    val ride = response.body() ?: return

                    pickupLat = ride.pickupLat
                    pickupLng = ride.pickupLon
                    dropLat   = ride.dropLat
                    dropLng   = ride.dropLon

                    txtInfo.text = """
                        Ride ID: ${ride.rideId}
                        Fare: ₹${ride.finalFare}
                        Status: ${ride.status}
                    """.trimIndent()

                    updateUIByStatus(ride.status)

                    if (ride.status == "CANCELLED") {
                        callSection.visibility   = View.GONE
                        txtCallUserTimer.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<RideDetailsResponse>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    private fun startTrip() {
        ApiClient.api.startTrip(authHeader, rideId)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {
                        fetchRideDetails()
                    } else {
                        toast("Failed to start trip")
                        sliderStartTrip.animate().x(0f).setDuration(150).start()
                    }
                }
                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    toast("Network error")
                    sliderStartTrip.animate().x(0f).setDuration(150).start()
                }
            })
    }

    private fun endTripAndGoToCollection(
        otp: String,
        dialog: AlertDialog,
        txtError: TextView,
        btnVerify: Button
    ) {
        ApiClient.api.endTrip(authHeader, rideId, EndRideRequest(otp))
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {

                        dialog.dismiss()
                        stopLocationUpdates()

                        val driverId = LocalStorage.getUserId(this@LiveRideActivity)

                        ApiClient.api.driverOnline(
                            authHeader,
                            driverId
                        ).enqueue(object : Callback<ResponseBody> {

                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                android.util.Log.d("ONLINE_API", "Driver Online Success")
                                checkPaymentAndOpenCollection()
                            }

                            override fun onFailure(
                                call: Call<ResponseBody>,
                                t: Throwable
                            ) {
                                android.util.Log.e("ONLINE_API", t.message ?: "Failed")
                                checkPaymentAndOpenCollection()
                            }
                        })
                    } else {
                        btnVerify.isEnabled = true
                        btnVerify.text = "Verify & End Trip"

                        val error = response.errorBody()?.string() ?: ""
                        txtError.text = if (error.contains("Incorrect OTP", ignoreCase = true)) {
                            "Incorrect OTP"
                        } else {
                            "Failed to end trip"
                        }
                        txtError.visibility = View.VISIBLE
                    }
                }
                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify & End Trip"
                    txtError.text = "Network error"
                    txtError.visibility = View.VISIBLE
                }
            })
    }

    private fun checkPaymentAndOpenCollection() {
        ApiClient.api.checkPaymentStatus(authHeader, rideId)
            .enqueue(object : Callback<Boolean> {
                override fun onResponse(call: Call<Boolean>, paymentResponse: Response<Boolean>) {
                    val isPaid = paymentResponse.body() ?: false

                    val intent = Intent(this@LiveRideActivity, CollectPaymentActivity::class.java)
                    intent.putExtra("rideId", rideId)
                    intent.putExtra("isPaid", isPaid)
                    startActivity(intent)
                    finish()
                }
                override fun onFailure(call: Call<Boolean>, t: Throwable) {
                    toast("Failed to check payment")
                }
            })
    }

    // ================= UI =================

    private fun resetButtons() {
        btnNavigatePickup.visibility = View.GONE
        btnNavigateDrop.visibility   = View.GONE
        dragStartTrip.visibility     = View.GONE
        dragEndTrip.visibility       = View.GONE
    }

    private fun updateUIByStatus(status: String) {
        resetButtons()

        when (status) {
            "ACCEPTED" -> {
                btnNavigatePickup.visibility = View.VISIBLE
                dragStartTrip.visibility     = View.VISIBLE
                showPickupOnMap()
                startLocationUpdates()
            }
            "IN_PROGRESS", "STARTED" -> {
                btnNavigateDrop.visibility = View.VISIBLE
                dragEndTrip.visibility     = View.VISIBLE
                showDropOnMap()
                startLocationUpdates()
            }
        }
    }

    // ================= MAP =================

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        googleMap.uiSettings.isZoomControlsEnabled = true
        val jhansi = LatLng(25.4484, 78.5685)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(jhansi, 13f))
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

    // ================= NAVIGATION =================

    private fun openGoogleMaps(lat: Double, lng: Double) {
        if (lat == 0.0 || lng == 0.0) {
            toast("Location not available")
            return
        }
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    // ================= BACKEND CALL =================

    private fun callUserViaBackend() {
        ApiClient.api.callRideConnect(authHeader, CallRideRequest(rideId))
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    toast("Please wait, we will connect your call\nकॉल कनेक्ट हो रही है, कृपया प्रतीक्षा करें...")
                    startCallUserTimer()
                }
                override fun onFailure(call: Call<String>, t: Throwable) {
                    startCallUserTimer()
                }
            })
    }

    // ================= TIMERS =================

    private fun startCallSenderTimer() {
        btnCallPickupPerson.isEnabled = false
        btnCallPickupPerson.text = "60s"
        callSenderTimer?.cancel()
        callSenderTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                btnCallPickupPerson.text = "${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                btnCallPickupPerson.isEnabled = true
                btnCallPickupPerson.text = "📞 Call"
            }
        }.start()
    }

    private fun startCallReceiverTimer() {
        btnCallDropPerson.isEnabled = false
        btnCallDropPerson.text = "60s"
        callReceiverTimer?.cancel()
        callReceiverTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                btnCallDropPerson.text = "${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                btnCallDropPerson.isEnabled = true
                btnCallDropPerson.text = "📞 Call"
            }
        }.start()
    }

    private fun startCallUserTimer() {
        btnCallUser.isEnabled = false
        btnCallUser.visibility = View.INVISIBLE
        txtCallUserTimer.visibility = View.VISIBLE
        callUserTimer?.cancel()
        callUserTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                txtCallUserTimer.text = "${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                btnCallUser.isEnabled = true
                btnCallUser.visibility = View.VISIBLE
                txtCallUserTimer.visibility = View.GONE
                txtCallUserTimer.text = ""
            }
        }.start()
    }

    // ================= CLEANUP =================

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        callSenderTimer?.cancel()
        callReceiverTimer?.cancel()
        callUserTimer?.cancel()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}