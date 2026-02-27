package com.zarkit.zarkit_partner

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.Entites.body
import com.zarkit.zarkit_partner.api.FareResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollectPaymentActivity : AppCompatActivity() {

    private var driverId: Long = -1L
    private lateinit var authHeader: String
    private var amount: Long = 0L
    private var isSubmitting = false

    private lateinit var txtFare: TextView
    private lateinit var txtRideFare: TextView
    private lateinit var txtTotal: TextView
    private lateinit var btnQr: Button

    private lateinit var dragCash: View
    private lateinit var sliderCash: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collect_payment)

        txtFare = findViewById(R.id.txtFare)
        txtRideFare = findViewById(R.id.txtRideFare)
        txtTotal = findViewById(R.id.txtTotal)
        btnQr = findViewById(R.id.btnQr)

        dragCash = findViewById(R.id.dragCash)
        sliderCash = findViewById(R.id.sliderCash)

        btnQr.isEnabled = false
        dragCash.visibility = View.GONE
        sliderCash.isEnabled = false

        // ---------- TOKEN ----------
        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            toast("Session expired")
            finish()
            return
        }
        authHeader = "Bearer $token"

        // ---------- DRIVER ----------
        driverId = LocalStorage.getUserId(this)
        if (driverId <= 0) {
            toast("Invalid driver")
            finish()
            return
        }

        val isPaid = intent.getBooleanExtra("isPaid", false)

        if (isPaid) {
            fetchFareThenShowPopup()
        } else {
            fetchLatestFare()
        }

        btnQr.setOnClickListener {

            val rideId = LocalStorage.getActiveRideId(this)

            if (rideId == 0L) {
                toast("Ride not found")
                return@setOnClickListener
            }

            val intent = Intent(this, QrPaymentActivity::class.java)
            intent.putExtra("amount", amount)
            intent.putExtra("rideId", rideId)
            startActivity(intent)
        }

        setupDragButton(sliderCash, dragCash) {
            if (!isSubmitting) {
                isSubmitting = true
                sliderCash.isEnabled = false
                callCashCollectedApi()
            }
        }
    }

    // ================= FETCH FARE FOR POPUP =================

    private fun fetchFareThenShowPopup() {
        ApiClient.api.getLatestFare(authHeader, driverId)
            .enqueue(object : Callback<FareResponse> {
                override fun onResponse(
                    call: Call<FareResponse>,
                    response: Response<FareResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        amount = response.body()!!.finalFare
                        showPaymentSuccessPopup()
                    } else {
                        toast("Failed to fetch fare")
                    }
                }

                override fun onFailure(call: Call<FareResponse>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    // ================= POPUP =================

    private fun showPaymentSuccessPopup() {
        AlertDialog.Builder(this)
            .setTitle("Payment Completed")
            .setMessage("This ride payment is successfully completed by Razorpay.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                callOnlineCollectApi()
            }
            .show()
    }

    // ================= ONLINE PAYMENT API =================

    private fun callOnlineCollectApi() {

        if (isSubmitting) return
        isSubmitting = true

        val request = body(driverId, amount)

        ApiClient.api.collectOnlinePayment(authHeader, request)
            .enqueue(object : Callback<Void> {

                override fun onResponse(call: Call<Void>, response: Response<Void>) {

                    isSubmitting = false

                    if (response.code() == 200) {

                        val intent = Intent(
                            this@CollectPaymentActivity,
                            DashboardActivity::class.java
                        )
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                        startActivity(intent)
                        finish()

                    } else {
                        toast("Error ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    isSubmitting = false
                    toast("Network error: ${t.message}")
                }
            })
    }

    // ================= NORMAL FARE =================

    private fun fetchLatestFare() {
        ApiClient.api.getLatestFare(authHeader, driverId)
            .enqueue(object : Callback<FareResponse> {
                override fun onResponse(
                    call: Call<FareResponse>,
                    response: Response<FareResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        amount = response.body()!!.finalFare

                        txtFare.text = "Collect ₹$amount"
                        txtRideFare.text = "₹$amount"
                        txtTotal.text = "₹$amount"

                        btnQr.isEnabled = true
                        dragCash.visibility = View.VISIBLE
                        sliderCash.isEnabled = true
                    } else {
                        toast("Failed to fetch fare")
                    }
                }

                override fun onFailure(call: Call<FareResponse>, t: Throwable) {
                    toast("Network error")
                }
            })
    }

    // ================= CASH API =================

    private fun callCashCollectedApi() {

        val request = body(driverId, amount)

        ApiClient.api.collectCashPayment(authHeader, request)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {

                    isSubmitting = false

                    if (response.isSuccessful) {

                        val message = response.body()?.string()
                            ?: "Cash collected successfully"

                        toast(message)

                        goToDashboard()

                    } else {
                        resetSlider()
                        toast("Error ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<ResponseBody>,
                    t: Throwable
                ) {
                    isSubmitting = false
                    resetSlider()
                    toast("Error: ${t.message}")
                }
            })
    }


    // ================= DRAG =================

    private fun setupDragButton(
        slider: View,
        parent: View,
        onComplete: () -> Unit
    ) {
        var downX = 0f

        slider.setOnTouchListener { v, event ->
            if (!v.isEnabled) return@setOnTouchListener false

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = v.x + (event.x - downX)
                    val maxX = (parent.width - v.width).toFloat()
                    v.x = newX.coerceIn(0f, maxX)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val completed = v.x > parent.width * 0.7f
                    if (completed) {
                        v.animate()
                            .x((parent.width - v.width).toFloat())
                            .setDuration(180)
                            .withEndAction { onComplete() }
                            .start()
                    } else {
                        v.animate().x(0f).setDuration(180).start()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun resetSlider() {
        sliderCash.animate().x(0f).setDuration(180).start()
        sliderCash.isEnabled = true
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
