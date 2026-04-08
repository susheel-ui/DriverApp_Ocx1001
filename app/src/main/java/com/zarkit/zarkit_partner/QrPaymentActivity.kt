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
import android.util.Log

class QrPaymentActivity : AppCompatActivity() {

    private val TAG = "QrPaymentActivity"

    private lateinit var binding: ActivityQrPaymentBinding
    private lateinit var authHeader: String

    private var transactionId: Long = 0L
    private var amount: Long = 0L
    private var rideId: Long = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val pollDelay = 5000L
    private var isPolling = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isPolling) {
                Log.d(TAG, "pollRunnable: isPolling is false, stopping")
                return
            }
            Log.d(TAG, "pollRunnable: running checkPaymentStatus")
            checkPaymentStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: activity started")

        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "onCreate: token is null or empty, finishing")
            toast("Session expired")
            finish()
            return
        }
        authHeader = "Bearer $token"
        Log.d(TAG, "onCreate: token loaded successfully")

        amount = intent.getLongExtra("amount", 0L)
        rideId = intent.getLongExtra("rideId", 0L)
        Log.d(TAG, "onCreate: amount=$amount, rideId=$rideId")

        if (amount == 0L || rideId == 0L) {
            Log.e(TAG, "onCreate: invalid amount or rideId, finishing")
            toast("Invalid payment data")
            finish()
            return
        }

        createQr()
    }

    // ================= CREATE QR =================

    private fun createQr() {
        Log.d(TAG, "createQr: sending request — amount=$amount, rideId=$rideId")
        val request = CreateQrRequest(amount, rideId)

        ApiClient.api.createQr(authHeader, request)
            .enqueue(object : Callback<QrResponse> {

                override fun onResponse(call: Call<QrResponse>, response: Response<QrResponse>) {
                    Log.d(TAG, "createQr: response code=${response.code()}")
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        transactionId = body.transactionId
                        Log.d(TAG, "createQr: success — transactionId=$transactionId, qrUrl=${body.qrUrl}")

                        binding.qrLoader.visibility = View.VISIBLE
                        downloadQrImage(body.qrUrl)
                        startPolling()

                    } else {
                        Log.e(TAG, "createQr: failed — code=${response.code()}, error=${response.errorBody()?.string()}")
                        toast("QR creation failed")
                        finish()
                    }
                }

                override fun onFailure(call: Call<QrResponse>, t: Throwable) {
                    Log.e(TAG, "createQr: network failure — ${t.message}", t)
                    toast("Network error")
                    finish()
                }
            })
    }

    // ================= QR IMAGE DOWNLOAD =================

    private fun downloadQrImage(url: String) {
        Log.d(TAG, "downloadQrImage: downloading from url=$url")
        Thread {
            try {
                val bitmap: Bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                Log.d(TAG, "downloadQrImage: bitmap downloaded successfully")
                runOnUiThread {
                    binding.imgQr.setImageBitmap(bitmap)
                    binding.qrLoader.visibility = View.GONE
                    Log.d(TAG, "downloadQrImage: QR image set on ImageView")
                }
            } catch (e: Exception) {
                Log.e(TAG, "downloadQrImage: failed to download image — ${e.message}", e)
                runOnUiThread {
                    binding.qrLoader.visibility = View.GONE
                    toast("Failed to load QR image")
                }
            }
        }.start()
    }

    // ================= POLLING =================

    private fun startPolling() {
        Log.d(TAG, "startPolling: polling started for transactionId=$transactionId")
        isPolling = true
        scheduleNextPoll()
    }

    private fun stopPolling() {
        Log.d(TAG, "stopPolling: stopping polling")
        isPolling = false
        handler.removeCallbacks(pollRunnable)
    }

    private fun scheduleNextPoll() {
        if (!isPolling) {
            Log.d(TAG, "scheduleNextPoll: isPolling=false, skipping schedule")
            return
        }
        Log.d(TAG, "scheduleNextPoll: next poll in ${pollDelay}ms")
        handler.postDelayed(pollRunnable, pollDelay)
    }

    private fun checkPaymentStatus() {
        Log.d(TAG, "checkPaymentStatus: checking status for transactionId=$transactionId")

        ApiClient.api.getPaymentStatus(authHeader, transactionId)
            .enqueue(object : Callback<PaymentStatusResponse> {

                override fun onResponse(
                    call: Call<PaymentStatusResponse>,
                    response: Response<PaymentStatusResponse>
                ) {
                    Log.d(TAG, "checkPaymentStatus: response code=${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val status = response.body()!!.status
                        Log.d(TAG, "checkPaymentStatus: status=$status")

                        when (status) {
                            "SUCCESS" -> {
                                Log.d(TAG, "checkPaymentStatus: SUCCESS — stopping polling and redirecting")
                                stopPolling()
                                toast("Payment Received")
                                LocalStorage.clearActiveRideId(this@QrPaymentActivity)
                                goToDashboard()
                            }

                            "FAILED" -> {
                                Log.e(TAG, "checkPaymentStatus: FAILED — stopping polling")
                                stopPolling()
                                toast("Payment Failed")
                            }

                            else -> {
                                Log.d(TAG, "checkPaymentStatus: status=$status (pending) — scheduling next poll")
                                scheduleNextPoll()
                            }
                        }
                    } else {
                        Log.w(TAG, "checkPaymentStatus: unsuccessful response — code=${response.code()}, retrying")
                        scheduleNextPoll()
                    }
                }

                override fun onFailure(call: Call<PaymentStatusResponse>, t: Throwable) {
                    Log.e(TAG, "checkPaymentStatus: network failure — ${t.message}, retrying", t)
                    scheduleNextPoll()
                }
            })
    }

    // ================= HELPERS =================

    private fun goToDashboard() {
        Log.d(TAG, "goToDashboard: navigating to DashboardActivity")
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
        Log.d(TAG, "onDestroy: activity destroyed, stopping polling")
        stopPolling()
    }
}