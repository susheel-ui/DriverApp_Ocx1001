package com.example.ocx_1001_driverapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.Entites.body
import com.example.ocx_1001_driverapp.api.FareResponse
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollectPaymentActivity : AppCompatActivity() {

    private var driverId: Long = -1L
    private lateinit var authHeader: String

    private var amount: Long = 0L
    private var isSubmitting = false

    private lateinit var txtFare: TextView
    private lateinit var btnCash: Button
    private lateinit var btnQr: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collect_payment)

        txtFare = findViewById(R.id.txtFare)
        btnCash = findViewById(R.id.btnCash)
        btnQr = findViewById(R.id.btnQr)

        btnCash.isEnabled = false
        btnQr.isEnabled = false

        // ---------- TOKEN ----------
        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            toast("Session expired")
            finish()
            return
        }
        authHeader = "Bearer $token"

        // ---------- DRIVER ID ----------
        driverId = LocalStorage.getUserId(this)
        if (driverId <= 0) {
            toast("Invalid driver")
            finish()
            return
        }

        // ---------- FETCH FARE ----------
        fetchLatestFare()

        btnCash.setOnClickListener {
            if (!isSubmitting) {
                isSubmitting = true
                callCashCollectedApi()
            }
        }

        btnQr.setOnClickListener {
            startActivity(
                Intent(this, QrPaymentActivity::class.java)
            )
        }
    }

    private fun fetchLatestFare() {
        ApiClient.api.getLatestFare(authHeader, driverId)
            .enqueue(object : Callback<FareResponse> {

                override fun onResponse(
                    call: Call<FareResponse>,
                    response: Response<FareResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        amount = response.body()!!.finalFare
                        txtFare.text = "₹$amount"

                        btnCash.isEnabled = true
                        btnQr.isEnabled = true
                    } else {
                        toast("Failed to fetch fare")
                    }
                }

                override fun onFailure(call: Call<FareResponse>, t: Throwable) {
                    toast("Network error while fetching fare")
                }
            })
    }

    private fun callCashCollectedApi() {

       val bdy = body(driverId, amount)


        ApiClient.api.cashCollected(authHeader, bdy)
            .enqueue(object : Callback<Void> {

                override fun onResponse(
                    call: Call<Void>,
                    response: Response<Void>
                ) {
                    if (response.isSuccessful) {
                        goToDashboard()
                    } else {
                        isSubmitting = false
                        toast("Error ${response.code()}")
                    }
                }


                override fun onFailure(call: Call<Void>, t: Throwable) {
                    isSubmitting = false
                    toast("Network error")
                }
            })
    }

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
}
