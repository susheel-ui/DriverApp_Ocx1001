package com.zarkit.zarkit_partner

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.zarkit.zarkit_partner.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RideRequestPopupActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var timer: CountDownTimer? = null

    private var acceptInProgress = false
    private var rideId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_ride_popup)

        // 🔒 Disable back
        onBackPressedDispatcher.addCallback(this) {}

        // ================= UI =================
        val txtFare = findViewById<TextView>(R.id.txtFare)
        val txtPickup = findViewById<TextView>(R.id.txtPickup)
        val txtDrop = findViewById<TextView>(R.id.txtDrop)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnReject = findViewById<Button>(R.id.btnReject)

        // ================= DATA =================
        rideId = intent.getLongExtra("rideId", -1L)
        val pickup = intent.getStringExtra("pickup") ?: "Pickup Location"
        val drop = intent.getStringExtra("drop") ?: "Drop Location"
        val fare = intent.getStringExtra("fare") ?: "--"

        txtFare.text = "₹$fare"
        txtPickup.text = pickup
        txtDrop.text = drop

        // ================= ALERTS =================
        stopAll()
        startSound()
        startVibration()
        startTimer(btnReject)

        // ================= ACCEPT =================
        btnAccept.setOnClickListener {

            if (acceptInProgress) return@setOnClickListener
            acceptInProgress = true
            btnAccept.isEnabled = false

            if (rideId == -1L) {
                Toast.makeText(this, "Invalid ride", Toast.LENGTH_SHORT).show()
                stopAll()
                finish()
                return@setOnClickListener
            }

            val tokenValue = LocalStorage.getToken(this)
            if (tokenValue.isNullOrEmpty()) {
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
                stopAll()
                finish()
                return@setOnClickListener
            }

            val token = "Bearer $tokenValue"

            ApiClient.api.acceptRide(token, rideId)
                .enqueue(object : Callback<Map<String, Any>> {

                    override fun onResponse(
                        call: Call<Map<String, Any>>,
                        response: Response<Map<String, Any>>
                    ) {
                        stopAll()

                        if (response.isSuccessful) {

                            // Save active ride
                            LocalStorage.saveActiveRideId(
                                this@RideRequestPopupActivity,
                                rideId
                            )

                            // Start foreground location service
                            val locationIntent = Intent(
                                this@RideRequestPopupActivity,
                                DriverLocationService::class.java
                            )

                            ContextCompat.startForegroundService(
                                this@RideRequestPopupActivity,
                                locationIntent
                            )

                            Toast.makeText(
                                this@RideRequestPopupActivity,
                                "Ride Accepted",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(
                                this@RideRequestPopupActivity,
                                LiveRideActivity::class.java
                            )
                            intent.putExtra("rideId", rideId)
                            startActivity(intent)

                        } else {
                            btnAccept.isEnabled = true
                            acceptInProgress = false

                            Toast.makeText(
                                this@RideRequestPopupActivity,
                                "Ride not available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        finish()
                    }

                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                        btnAccept.isEnabled = true
                        acceptInProgress = false

                        Toast.makeText(
                            this@RideRequestPopupActivity,
                            "Network error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        // ================= REJECT =================
        btnReject.setOnClickListener {
            stopAll()
            finish()
        }
    }

    // ================= SOUND =================
    private fun startSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(
                this@RideRequestPopupActivity,
                android.net.Uri.parse(
                    "android.resource://${packageName}/${R.raw.ride_request_tone}"
                )
            )
            isLooping = true
            prepare()
            start()
        }
    }

    // ================= VIBRATION =================
    private fun startVibration() {
        vibrator?.cancel()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 800, 800),
                    0
                )
            )
        } else {
            vibrator?.vibrate(longArrayOf(0, 800, 800), 0)
        }
    }

    // ================= TIMER =================
    private fun startTimer(btnReject: Button) {
        timer?.cancel()
        timer = object : CountDownTimer(30_000, 1000) {

            override fun onTick(ms: Long) {
                btnReject.text = "REJECT (${ms / 1000}s)"
            }

            override fun onFinish() {
                stopAll()
                finish()
            }

        }.start()
    }

    // ================= STOP ALL =================
    private fun stopAll() {
        timer?.cancel()
        timer = null

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }
}
