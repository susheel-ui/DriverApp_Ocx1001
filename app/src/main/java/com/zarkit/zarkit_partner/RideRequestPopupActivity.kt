package com.zarkit.zarkit_partner

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.zarkit.zarkit_partner.api.ApiClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RideRequestPopupActivity : BaseActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var timer: CountDownTimer? = null
    private var acceptInProgress = false
    private var rideId: Long = -1L

    private lateinit var progressBar: ProgressBar
    private lateinit var tvTimerLabel: TextView
    private lateinit var btnReject: Button
    private lateinit var btnAccept: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_ride_popup)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Disable back button
        onBackPressedDispatcher.addCallback(this) {}

        // ================= UI =================
        val txtFare    = findViewById<TextView>(R.id.txtFare)
        val txtPickup  = findViewById<TextView>(R.id.txtPickup)
        val txtDrop    = findViewById<TextView>(R.id.txtDrop)
        btnAccept      = findViewById(R.id.btnAccept)
        btnReject      = findViewById(R.id.btnReject)
        progressBar    = findViewById(R.id.timerProgressBar)
        tvTimerLabel   = findViewById(R.id.tvTimerLabel)

        // ================= DATA =================
        rideId = intent.getLongExtra("rideId", -1L)
        val pickup = intent.getStringExtra("pickup") ?: "Pickup Location"
        val drop   = intent.getStringExtra("drop")   ?: "Drop Location"
        val fare   = intent.getStringExtra("fare")   ?: "--"

        txtFare.text   = "₹$fare"
        txtPickup.text = pickup
        txtDrop.text   = drop

        // ================= ALERTS =================
        stopAll()
        startSound()
        startVibration()
        startAutoRejectTimer()

        // ================= ACCEPT =================
        btnAccept.setOnClickListener {

            if (acceptInProgress) return@setOnClickListener
            acceptInProgress = true
            btnAccept.isEnabled = false

            if (rideId == -1L) {
                Toast.makeText(this, "Invalid ride", Toast.LENGTH_SHORT).show()
                dismissAll()
                finish()
                return@setOnClickListener
            }

            val tokenValue = LocalStorage.getToken(this)
            if (tokenValue.isNullOrEmpty()) {
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
                dismissAll()
                finish()
                return@setOnClickListener
            }

            ApiClient.api.acceptRide("Bearer $tokenValue", rideId)
                .enqueue(object : Callback<Map<String, Any>> {

                    override fun onResponse(
                        call: Call<Map<String, Any>>,
                        response: Response<Map<String, Any>>
                    ) {
                        dismissAll()

                        if (response.isSuccessful) {

                            LocalStorage.saveActiveRideId(this@RideRequestPopupActivity, rideId)
                            val driverId = LocalStorage.getUserId(this@RideRequestPopupActivity)

                            ApiClient.api.driverOffline(
                                "Bearer $tokenValue",
                                driverId
                            ).enqueue(object : Callback<ResponseBody> {

                                override fun onResponse(
                                    call: Call<ResponseBody>,
                                    response: Response<ResponseBody>
                                ) {
                                }

                                override fun onFailure(
                                    call: Call<ResponseBody>,
                                    t: Throwable
                                ) {
                                }
                            })

                            ContextCompat.startForegroundService(
                                this@RideRequestPopupActivity,
                                Intent(this@RideRequestPopupActivity, DriverLocationService::class.java)
                            )

                            Toast.makeText(this@RideRequestPopupActivity, "Ride Accepted ✅", Toast.LENGTH_SHORT).show()

                            startActivity(
                                Intent(this@RideRequestPopupActivity, LiveRideActivity::class.java).apply {
                                    putExtra("rideId", rideId)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )

                        } else {
                            btnAccept.isEnabled = true
                            acceptInProgress = false
                            Toast.makeText(this@RideRequestPopupActivity, "Ride not available", Toast.LENGTH_SHORT).show()
                        }

                        finish()
                    }

                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                        btnAccept.isEnabled = true
                        acceptInProgress = false
                        Toast.makeText(this@RideRequestPopupActivity, "Network error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // ================= REJECT =================
        btnReject.setOnClickListener {
            dismissAll()
            finish()
        }
    }

    // ================= AUTO REJECT TIMER =================
    private fun startAutoRejectTimer() {
        val TOTAL_SECONDS = 30

        progressBar.max      = TOTAL_SECONDS
        progressBar.progress = TOTAL_SECONDS

        timer?.cancel()
        timer = object : CountDownTimer(TOTAL_SECONDS * 1000L, 1000L) {

            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                progressBar.progress = secondsLeft
                tvTimerLabel.text    = "ऑटो रिजेक्ट टाइमर: $secondsLeft सेकेंड"
                btnReject.text       = "REJECT (${secondsLeft}s)"
            }

            override fun onFinish() {
                progressBar.progress = 0
                tvTimerLabel.text    = "ऑटो रिजेक्ट टाइमर: 0 सेकेंड"
                dismissAll()
                finish()
            }

        }.start()
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
                android.net.Uri.parse("android.resource://${packageName}/${R.raw.ride_request_tone}")
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
                VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 800, 800), 0)
        }
    }

    // ================= DISMISS ALL =================
    private fun dismissAll() {
        stopAll()
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(RideForegroundService.NOTIFICATION_ID)
        nm.cancel(RideForegroundService.FOREGROUND_ID)
        nm.cancelAll()
        stopService(Intent(this, RideForegroundService::class.java))
    }

    // ================= STOP ALL =================
    private fun stopAll() {
        timer?.cancel()
        timer = null

        try { mediaPlayer?.stop() } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }
}