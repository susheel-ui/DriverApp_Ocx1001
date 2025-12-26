package com.example.ocx_1001_driverapp

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RideRequestPopupActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Android 8+ (Ola/Uber style)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // ✅ Old Android
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_ride_popup)

        val txtMessage = findViewById<TextView>(R.id.txtMessage)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnReject = findViewById<Button>(R.id.btnReject)

        val pickup = intent.getStringExtra("pickup") ?: "Pickup"
        val drop = intent.getStringExtra("drop") ?: "Drop"
        val rideId = intent.getLongExtra("rideId", -1L)

        txtMessage.text = "New Ride Request\n$pickup → $drop"

        startSound()
        startVibration()
        startTimer(btnReject)

        // ================= ACCEPT =================
        btnAccept.setOnClickListener {

            if (rideId == -1L) {
                Toast.makeText(this, "Invalid ride", Toast.LENGTH_SHORT).show()
                stopAll()
                finish()
                return@setOnClickListener
            }

            btnAccept.isEnabled = false // prevent double tap

            ApiClient.api.acceptRide(rideId)
                .enqueue(object : Callback<Map<String, Any>> {

                    override fun onResponse(
                        call: Call<Map<String, Any>>,
                        response: Response<Map<String, Any>>
                    ) {
                        stopAll()

                        if (response.isSuccessful) {
                            // ✅ YOU GOT THE RIDE
                            Toast.makeText(
                                this@RideRequestPopupActivity,
                                "Ride accepted",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // ❌ SOMEONE ELSE ACCEPTED
                            Toast.makeText(
                                this@RideRequestPopupActivity,
                                "Someone already accepted this ride",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    }

                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                        stopAll()
                        Toast.makeText(
                            this@RideRequestPopupActivity,
                            "Network error",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                })
        }

        // ================= REJECT =================
        btnReject.setOnClickListener {
            stopAll()
            finish()
        }
    }

    // 🔊 Call-style sound
    private fun startSound() {
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

    // 📳 Vibration
    private fun startVibration() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 1000, 1000),
                    0
                )
            )
        } else {
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        }
    }

    // ⏱ Auto timeout
    private fun startTimer(btnReject: Button) {
        timer = object : CountDownTimer(15_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                btnReject.text = "REJECT (${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                stopAll()
                finish()
            }
        }.start()
    }

    private fun stopAll() {
        timer?.cancel()

        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }
    @Deprecated("Back disabled intentionally", level = DeprecationLevel.HIDDEN)
    override fun onBackPressed() {
        // ❌ Disable back
    }
}
