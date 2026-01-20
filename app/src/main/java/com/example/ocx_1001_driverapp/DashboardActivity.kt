package com.example.ocx_1001_driverapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.DriverEarningResponse
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.math.abs

class DashboardActivity : AppCompatActivity(), PaymentResultListener {

    private var isOnline = false
    private var blockGoOnline = false
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var txtEarning: TextView
    private lateinit var txtCommission: TextView
    private lateinit var txtDriverName: TextView
    private lateinit var btnPayNow: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ================= UI =================
        drawerLayout = findViewById(R.id.drawerLayout)

        val goOnlineLayout = findViewById<RelativeLayout>(R.id.btnGoOnline)
        val sliderCircle = findViewById<ImageView>(R.id.sliderCircle)
        val txtStatus = findViewById<TextView>(R.id.txtOnlineStatus)
        val statusDot = findViewById<ImageView>(R.id.statusDot)

        txtEarning = findViewById(R.id.txtEarning)
        txtCommission = findViewById(R.id.txtCommission)
        txtDriverName = findViewById(R.id.txtDriverName)
        btnPayNow = findViewById(R.id.btnPayNow)

        btnPayNow.visibility = View.GONE
        btnPayNow.setOnClickListener { startRazorpayPayment() }

        // ================= DRAWER OPEN =================
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<TextView>(R.id.menuLogout).setOnClickListener {
            drawerLayout.closeDrawers()
            logout()
        }

        // ================= ONLINE / OFFLINE =================
        goOnlineLayout.setBackgroundResource(R.drawable.btn_offline)
        txtStatus.text = "GO ONLINE"
        statusDot.setBackgroundResource(R.drawable.status_dot_red)

        goOnlineLayout.setOnClickListener {

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
                sliderCircle.animate().translationX(moveX.toFloat()).setDuration(250).start()
                goOnlineLayout.setBackgroundResource(R.drawable.btn_online)
                txtStatus.text = "GO OFFLINE"
                statusDot.setBackgroundResource(R.drawable.status_dot_green)
                isOnline = true
            } else {
                sliderCircle.animate().translationX(0f).setDuration(250).start()
                goOnlineLayout.setBackgroundResource(R.drawable.btn_offline)
                txtStatus.text = "GO ONLINE"
                statusDot.setBackgroundResource(R.drawable.status_dot_red)
                isOnline = false
            }
        }

        // ================= API =================
        fetchDriverEarning()
    }

    override fun onStart() {
        super.onStart()
        if (LocalStorage.getToken(this).isNullOrEmpty()) logout()
    }

    // ================= FETCH EARNING =================
    private fun fetchDriverEarning() {

        val token = LocalStorage.getToken(this)
        val driverId = LocalStorage.getUserId(this)

        if (token.isNullOrEmpty() || driverId == 0L) {
            logout()
            return
        }

        ApiClient.api
            .getDriverEarningByDriverId("Bearer $token", driverId)
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

                    txtEarning.text = "₹${formatAmount(data.earning)}"
                    txtCommission.text = "₹${formatAmount(data.commission)}"
                    txtDriverName.text = "${data.firstName} ${data.lastName}"

                    handleEarningRules(data.earning, data.commission)
                }

                override fun onFailure(call: Call<DriverEarningResponse>, t: Throwable) {
                    Log.e("EARNING_API", "Network error", t)
                }
            })
    }

    // ================= BUSINESS RULES =================
    private fun handleEarningRules(earning: Double, commission: Double) {
        when {
            earning == 0.0 && commission == -500.0 -> {
                blockGoOnline = true
                btnPayNow.visibility = View.VISIBLE
                showPopup("Registration Fee Pending", "Please pay registration fees.")
            }

            earning != 0.0 && commission < -500.0 -> {
                blockGoOnline = true
                btnPayNow.visibility = View.VISIBLE
                showPopup("Commission Due", "Please clear your commission.")
            }

            else -> {
                blockGoOnline = false
                btnPayNow.visibility = View.GONE
            }
        }
    }

    // ================= RAZORPAY =================
    private fun startRazorpayPayment() {

        val checkout = Checkout()
        checkout.setKeyID("rzp_test_S5hfApqAqe4arW") // replace with your key

        val commissionValue = abs(
            txtCommission.text.toString()
                .replace("₹", "")
                .toDouble()
        )

        val amountInPaise = (commissionValue * 100).toInt()

        try {
            val options = JSONObject()
            options.put("name", "ZARKIT PARTNER")
            options.put("description", "Commission Payment")
            options.put("currency", "INR")
            options.put("amount", amountInPaise)
            options.put("payment_capture", 1)

            checkout.open(this, options)

        } catch (e: Exception) {
            Toast.makeText(this, "Payment error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(paymentId: String?) {
        Log.i("RAZORPAY_PAYMENT", """
        Payment Success
        paymentId = $paymentId
        amount = ${txtCommission.text}
        time = ${System.currentTimeMillis()}
    """.trimIndent())
        if (paymentId == null) {
            Toast.makeText(this, "Payment ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        verifyPaymentWithBackend(paymentId)
    }


    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show()
    }

    // ================= HELPERS =================
    private fun formatAmount(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    // ================= LOGOUT =================
    private fun logout() {
        LocalStorage.clearActiveRideId(this)
        LocalStorage.saveToken(this, "")
        LocalStorage.saveUserId(this, 0)

        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun showPopup(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun verifyPaymentWithBackend(paymentId: String) {

        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        val request = com.example.ocx_1001_driverapp.api.RazorpayVerifyRequest(paymentId)

        ApiClient.api
            .verifyRazorpayPayment("Bearer $token", request)
            .enqueue(object : Callback<Void> {

                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Payment Verified Successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        blockGoOnline = false
                        btnPayNow.visibility = View.GONE
                        fetchDriverEarning()

                    } else {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Payment verification failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("PAY_VERIFY", "Backend error", t)
                    Toast.makeText(
                        this@DashboardActivity,
                        "Server error while verifying payment",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

}
