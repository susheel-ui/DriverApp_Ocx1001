package com.zarkit.zarkit_partner

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zarkit.zarkit_partner.api.*
import com.zarkit.zarkit_partner.databinding.ActivityQrPaymentBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL

class QrPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrPaymentBinding
    private lateinit var authHeader: String

    private var transactionId: Long = 0L
    private var amount: Long = 0L
    private var rideId: Long = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val pollDelay = 5000L // 5 sec polling

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 TOKEN
        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            toast("Session expired")
            finish()
            return
        }
        authHeader = "Bearer $token"

        // 🔹 DATA FROM PREVIOUS SCREEN
        amount = intent.getLongExtra("amount", 0L)
        rideId = intent.getLongExtra("rideId", 0L)

        if (amount == 0L || rideId == 0L) {
            toast("Invalid payment data")
            finish()
            return
        }

        createQr()
    }

    // ================= CREATE QR =================

    private fun createQr() {
        val request = CreateQrRequest(amount, rideId)

        ApiClient.api.createQr(authHeader, request)
            .enqueue(object : Callback<QrResponse> {

                override fun onResponse(
                    call: Call<QrResponse>,
                    response: Response<QrResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {

                        val body = response.body()!!
                        transactionId = body.transactionId

                        // ✅ Show loader before downloading
                        binding.qrLoader.visibility = View.VISIBLE
                        downloadQrImage(body.qrUrl)

                        startPolling()

                    } else {
                        toast("QR creation failed")
                        finish()
                    }
                }

                override fun onFailure(call: Call<QrResponse>, t: Throwable) {
                    toast("Network error")
                    finish()
                }
            })
    }

    // ================= MANUAL IMAGE DOWNLOAD =================

    private fun downloadQrImage(url: String) {
        Thread {
            try {
                val inputStream = URL(url).openStream()
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

                runOnUiThread {
                    binding.imgQr.setImageBitmap(bitmap)
                    binding.qrLoader.visibility = View.GONE // hide loader
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.qrLoader.visibility = View.GONE // hide loader
                    toast("Failed to load QR image")
                }
            }
        }.start()
    }

    // ================= POLLING =================

    private fun startPolling() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkPaymentStatus()
                handler.postDelayed(this, pollDelay)
            }
        }, pollDelay)
    }

    private fun checkPaymentStatus() {
        ApiClient.api.getPaymentStatus(authHeader, transactionId)
            .enqueue(object : Callback<PaymentStatusResponse> {

                override fun onResponse(
                    call: Call<PaymentStatusResponse>,
                    response: Response<PaymentStatusResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {

                        val status = response.body()!!.status

                        if (status == "SUCCESS") {

                            handler.removeCallbacksAndMessages(null)

                            toast("Payment Received")

                            LocalStorage.clearActiveRideId(this@QrPaymentActivity)

                            goToDashboard()

                        } else if (status == "FAILED") {

                            handler.removeCallbacksAndMessages(null)
                            toast("Payment Failed")
                        }
                    }
                }

                override fun onFailure(call: Call<PaymentStatusResponse>, t: Throwable) {
                    // silent retry
                }
            })
    }

    // ================= HELPERS =================

    private fun goToDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
