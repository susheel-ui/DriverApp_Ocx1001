package com.example.ocx_1001_driverapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.DriverEarningResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private var isOnline = false
    private var blockGoOnline = false   // 🔥 IMPORTANT FLAG

    private lateinit var txtEarning: TextView
    private lateinit var txtCommission: TextView
    private lateinit var txtDriverName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // UI refs
        val goOnlineLayout = findViewById<RelativeLayout>(R.id.btnGoOnline)
        val sliderCircle = findViewById<ImageView>(R.id.sliderCircle)
        val txtStatus = findViewById<TextView>(R.id.txtOnlineStatus)
        val statusDot = findViewById<ImageView>(R.id.statusDot)

        txtEarning = findViewById(R.id.txtEarning)
        txtCommission = findViewById(R.id.txtCommission)
        txtDriverName = findViewById(R.id.txtDriverName)

        // Default OFFLINE state
        goOnlineLayout.setBackgroundResource(R.drawable.btn_offline)
        txtStatus.text = "GO ONLINE"
        statusDot.setBackgroundResource(R.drawable.status_dot_red)

        goOnlineLayout.setOnClickListener {

            // 🚫 BLOCK GO ONLINE IF REQUIRED
            if (blockGoOnline) {
                Toast.makeText(
                    this,
                    "Please clear dues before going online",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val moveX = goOnlineLayout.width - sliderCircle.width - 12

            if (!isOnline) {
                sliderCircle.animate()
                    .translationX(moveX.toFloat())
                    .setDuration(250)
                    .start()

                goOnlineLayout.setBackgroundResource(R.drawable.btn_online)
                txtStatus.text = "GO OFFLINE"
                statusDot.setBackgroundResource(R.drawable.status_dot_green)
                isOnline = true
            } else {
                sliderCircle.animate()
                    .translationX(0f)
                    .setDuration(250)
                    .start()

                goOnlineLayout.setBackgroundResource(R.drawable.btn_offline)
                txtStatus.text = "GO ONLINE"
                statusDot.setBackgroundResource(R.drawable.status_dot_red)
                isOnline = false
            }
        }

        // 🔥 Fetch earning
        fetchDriverEarning()
    }

    private fun fetchDriverEarning() {

        val token = LocalStorage.getToken(this)
        val driverId = LocalStorage.getUserId(this)

        if (token.isNullOrEmpty() || driverId == 0L) {
            Toast.makeText(
                this,
                "Session expired. Please login again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        ApiClient.api
            .getDriverEarningByDriverId(driverId)
            .enqueue(object : Callback<DriverEarningResponse> {

                override fun onResponse(
                    call: Call<DriverEarningResponse>,
                    response: Response<DriverEarningResponse>
                ) {

                    if (!response.isSuccessful || response.body() == null) {
                        Log.e("EARNING_API", "Error code: ${response.code()}")
                        return
                    }

                    val data = response.body()!!

                    txtEarning.text = "₹${data.earning}"
                    txtCommission.text = "₹${data.commission}"
                    txtDriverName.text = "${data.firstName} ${data.lastName}"

                    // 🔥 APPLY BUSINESS RULES
                    handleEarningRules(data.earning, data.commission)
                }

                override fun onFailure(call: Call<DriverEarningResponse>, t: Throwable) {
                    Log.e("EARNING_API", t.message ?: "Network error")
                }
            })
    }

    // ============================
    // 🔥 BUSINESS LOGIC HANDLER
    // ============================
    private fun handleEarningRules(earning: Double, commission: Double) {

        when {
            earning == 0.0 && commission == -500.0 -> {
                blockGoOnline = true
                showPopup(
                    "Registration Fee Pending",
                    "Please pay registration fees, then you can start your work."
                )
            }

            earning != 0.0 && commission < -500.0 -> {
                blockGoOnline = true
                showPopup(
                    "Commission Due",
                    "Your commission is too much. Please clear the commission, then start work."
                )
            }

            else -> {
                blockGoOnline = false
            }
        }
    }

    // ============================
    // 🔔 POPUP DIALOG
    // ============================
    private fun showPopup(title: String, message: String) {

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
