package com.example.ocx_1001_driverapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RideRequestPopupActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen + wake display
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_ride_popup)

        val txtMessage = findViewById<TextView>(R.id.txtMessage)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnReject = findViewById<Button>(R.id.btnReject)

        val pickup = intent.getStringExtra("pickup") ?: "Pickup"
        val drop = intent.getStringExtra("drop") ?: "Drop"

        txtMessage.text = "New Ride Request:\n$pickup → $drop"

        // 🔊 RINGTONE
        mediaPlayer = MediaPlayer.create(this, R.raw.ride_request_tone)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // 📳 SAFE MAX VIBRATION (no crash on Vivo)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator?.vibrate(effect)
            } else {
                vibrator?.vibrate(800)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // TIMER
        timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millis: Long) {
                btnReject.text = "REJECT (${millis / 1000}s)"
            }

            override fun onFinish() {
                stopAlert()
                finish()
            }
        }.start()

        btnAccept.setOnClickListener {
            stopAlert()
            finish()
        }

        btnReject.setOnClickListener {
            stopAlert()
            finish()
        }
    }

    private fun stopAlert() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
        timer?.cancel()
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }
}
