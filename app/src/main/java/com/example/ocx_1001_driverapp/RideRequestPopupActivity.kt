package com.example.ocx_1001_driverapp

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
import com.example.ocx_1001_driverapp.api.ApiClient
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

        // 🔥 REQUIRED FOR LOCK SCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_ride_popup)

        // 🔒 DISABLE BACK BUTTON
        onBackPressedDispatcher.addCallback(this) {
            // disabled intentionally
        }

        val txtMessage = findViewById<TextView>(R.id.txtMessage)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnReject = findViewById<Button>(R.id.btnReject)

        // 🔥 READ DATA
        rideId = intent.getLongExtra("rideId", -1L)
        val pickup = intent.getStringExtra("pickup") ?: "Pickup"
        val drop = intent.getStringExtra("drop") ?: "Drop"
        val fare = intent.getStringExtra("fare") ?: "--"

        txtMessage.text =
            "New Ride Request\n$pickup → $drop\nFare: ₹$fare"

        // 🔥 RESET + START ALERTS
        stopAll()
        startSound()
        startVibration()
        startTimer(btnReject)

        // ================= ACCEPT =================
        btnAccept.setOnClickListener {

            if (acceptInProgress) return@setOnClickListener
            acceptInProgress = true

            if (rideId == -1L) {
                Toast.makeText(this, "Invalid ride", Toast.LENGTH_SHORT).show()
                stopAll()
                finish()
                return@setOnClickListener
            }

            btnAccept.isEnabled = false

            val token = "Bearer ${LocalStorage.getToken(this)}"

            ApiClient.api.acceptRide(token, rideId)
                .enqueue(object : Callback<Map<String, Any>> {

                    override fun onResponse(
                        call: Call<Map<String, Any>>,
                        response: Response<Map<String, Any>>
                    ) {
                        stopAll()

                        if (response.isSuccessful) {

                            // 🔥 SAVE ACTIVE RIDE
                            LocalStorage.saveActiveRideId(this@RideRequestPopupActivity, rideId)

                            Toast.makeText(
                                this@RideRequestPopupActivity,
                                "Ride accepted",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(
                                this@RideRequestPopupActivity,
                                LiveRideActivity::class.java
                            )
                            intent.putExtra("rideId", rideId)
                            startActivity(intent)
                        }
                        else {
                            Toast.makeText(
                                this@RideRequestPopupActivity,
                                "Ride not available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    }

                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                        acceptInProgress = false
                        btnAccept.isEnabled = true
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

    // 🔊 SOUND
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

    // 📳 VIBRATION
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

    // ⏱ TIMER
    private fun startTimer(btnReject: Button) {
        timer?.cancel()
        timer = object : CountDownTimer(15_000, 1000) {
            override fun onTick(ms: Long) {
                btnReject.text = "REJECT (${ms / 1000}s)"
            }

            override fun onFinish() {
                stopAll()
                finish()
            }
        }.start()
    }

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
