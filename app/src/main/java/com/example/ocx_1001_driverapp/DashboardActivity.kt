package com.example.ocx_1001_driverapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.DriverEarningResponse
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private var isOnline = false
    private var blockGoOnline = false

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerMenu: LinearLayout
    private lateinit var statusDot: ImageView
    private lateinit var txtEarning: TextView
    private lateinit var txtCommission: TextView
    private lateinit var txtDriverName: TextView
    private lateinit var btnPayNow: Button

    private lateinit var btnMenu: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ================= UI =================
        drawerLayout = findViewById(R.id.drawerLayout)
        drawerMenu = findViewById(R.id.drawerMenu)
        statusDot = findViewById(R.id.statusDot)
        btnMenu = findViewById(R.id.btnMenu)

        val btnProfile = findViewById<ImageView>(R.id.btnProfile)
        btnProfile.setOnClickListener {
            val intent = Intent(this, SupportActivity::class.java)
            startActivity(intent)
        }

        val txtViewProfile = findViewById<TextView>(R.id.txtViewProfile)
        txtViewProfile.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        val goOnlineLayout = findViewById<RelativeLayout>(R.id.btnGoOnline)
        val sliderCircle = findViewById<FrameLayout>(R.id.sliderContainer)
        val txtOnlineStatus = findViewById<TextView>(R.id.txtOnlineStatus)

        txtEarning = findViewById(R.id.txtEarning)
        txtCommission = findViewById(R.id.txtCommission)
        txtDriverName = findViewById(R.id.txtDriverName)
        btnPayNow = findViewById(R.id.btnPayNow)
        btnPayNow.visibility = View.GONE

        // ================= DRAWER =================
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(drawerMenu) }

        DrawerMenuHelper.setupDrawerMenu(drawerLayout, this) { logout() }

        // ================= ONLINE / OFFLINE =================
        goOnlineLayout.setBackgroundResource(R.drawable.btn_offline)
        txtOnlineStatus.text = "GO ONLINE"
        statusDot.setBackgroundResource(R.drawable.status_dot_red)
        setupSlider(sliderCircle, goOnlineLayout, txtOnlineStatus)

        // ================= PAY NOW BUTTON =================
        btnPayNow.setOnClickListener {
            PaymentHelper.startRazorpayPayment(this, txtCommission.text.toString()) { paymentId ->
                verifyPaymentWithBackend(paymentId)
            }
        }

        // ================= API =================
        fetchDriverEarning()
    }

    // ================= ACTIVE RIDE ID HELPER =================
    fun getActiveRideId(): Long {
        return LocalStorage.getActiveRideId(this)
    }

    // ================= SLIDER =================
    private fun setupSlider(slider: View, parent: RelativeLayout, statusText: TextView) {
        var dX = 0f
        slider.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    if (newX < parent.x) newX = parent.x
                    if (newX > parent.x + parent.width - view.width)
                        newX = parent.x + parent.width - view.width
                    view.x = newX
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val middle = parent.width / 2
                    if (view.x > middle) {
                        goOnline(view, parent, statusText)
                        isOnline = true
                    } else {
                        goOffline(view, parent, statusText)
                        isOnline = false
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun goOnline(slider: View, parent: RelativeLayout, statusText: TextView) {
        slider.animate().x(parent.width - slider.width.toFloat()).setDuration(200).start()
        statusText.text = "ONLINE"
        parent.setBackgroundResource(R.drawable.btn_online)
        statusDot.setBackgroundResource(R.drawable.status_dot_green)
        Toast.makeText(this, "You are ONLINE", Toast.LENGTH_SHORT).show()
    }

    private fun goOffline(slider: View, parent: RelativeLayout, statusText: TextView) {
        slider.animate().x(0f).setDuration(200).start()
        statusText.text = "GO ONLINE"
        parent.setBackgroundResource(R.drawable.btn_offline)
        statusDot.setBackgroundResource(R.drawable.status_dot_red)
        Toast.makeText(this, "You are OFFLINE", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        if (LocalStorage.getToken(this).isNullOrEmpty()) logout()
    }

    // ================= FETCH DRIVER EARNINGS =================
    private fun fetchDriverEarning() {
        val token = LocalStorage.getToken(this)
        val driverId = LocalStorage.getUserId(this)
        if (token.isNullOrEmpty() || driverId == 0L) {
            logout()
            return
        }

        ApiClient.api.getDriverEarningByDriverId("Bearer $token", driverId)
            .enqueue(object : retrofit2.Callback<DriverEarningResponse> {
                override fun onResponse(
                    call: retrofit2.Call<DriverEarningResponse>,
                    response: retrofit2.Response<DriverEarningResponse>
                ) {
                    val data = response.body() ?: return
                    txtEarning.text = "₹${formatAmount(data.earning)}"
                    txtCommission.text = "₹${formatAmount(data.commission)}"
                    txtDriverName.text = "${data.firstName} ${data.lastName}"
                    handleEarningRules(data.earning, data.commission)
                }

                override fun onFailure(call: retrofit2.Call<DriverEarningResponse>, t: Throwable) {
                    Log.e("EARNING_API", "Network error", t)
                }
            })
    }

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

    private fun verifyPaymentWithBackend(paymentId: String) {
        val token = LocalStorage.getToken(this)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        val request = com.example.ocx_1001_driverapp.api.RazorpayVerifyRequest(paymentId)
        ApiClient.api.verifyRazorpayPayment("Bearer $token", request)
            .enqueue(object : retrofit2.Callback<Void> {
                override fun onResponse(
                    call: retrofit2.Call<Void>,
                    response: retrofit2.Response<Void>
                ) {
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

                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    Log.e("PAY_VERIFY", "Backend error", t)
                    Toast.makeText(
                        this@DashboardActivity,
                        "Server error while verifying payment",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // ================= HELPERS =================
    private fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)

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

    fun showPopup(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}
