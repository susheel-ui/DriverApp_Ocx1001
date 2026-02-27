package com.zarkit.zarkit_partner

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.DriverEarningResponse
import com.zarkit.zarkit_partner.api.DriverVehicleInfoResponse
import com.zarkit.zarkit_partner.api.RazorpayOrderResponse
import com.razorpay.Checkout
import java.util.Locale
import kotlin.math.abs
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.zarkit.zarkit_partner.api.RazorpayVerifyRequest
import org.json.JSONObject

class DashboardActivity : AppCompatActivity(),PaymentResultWithDataListener {

    private var isOnline = false
    private var blockGoOnline = false

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerMenu: LinearLayout
    private lateinit var statusDot: ImageView
    private lateinit var txtEarning: TextView
    private lateinit var txtDriverName: TextView
    private lateinit var txtVehicleInfo: TextView
    private lateinit var btnPayNow: Button
    private lateinit var btnMenu: ImageView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent)) { view, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // ================= UI =================
        drawerLayout = findViewById(R.id.drawerLayout)
        drawerMenu = findViewById(R.id.drawerMenu)
        statusDot = findViewById(R.id.statusDot)
        btnMenu = findViewById(R.id.btnMenu)

        txtEarning = findViewById(R.id.txtEarning)
        txtDriverName = findViewById(R.id.txtDriverName)
        txtVehicleInfo = findViewById(R.id.txtVehicleInfo)
        btnPayNow = findViewById(R.id.btnPayNow)

        btnPayNow.visibility = View.GONE

        val btnProfile = findViewById<ImageView>(R.id.btnProfile)
        btnProfile.setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }

        val txtViewProfile = findViewById<TextView>(R.id.txtViewProfile)
        txtViewProfile.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        val goOnlineLayout = findViewById<RelativeLayout>(R.id.btnGoOnline)
        val sliderCircle = findViewById<FrameLayout>(R.id.sliderContainer)
        val txtOnlineStatus = findViewById<TextView>(R.id.txtOnlineStatus)
        val drawerMenu: LinearLayout = findViewById(R.id.drawerMenu)
        val menuWithdrawRequests: TextView = drawerMenu.findViewById(R.id.menuWithdrawRequests)


        menuWithdrawRequests.setOnClickListener {
            // Open the Withdraw Requests Activity
            startActivity(Intent(this, WithdrawRequestsActivity::class.java))
            drawerLayout.closeDrawers()
        }

        // ================= DRAWER =================
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(drawerMenu) }
        DrawerMenuHelper.setupDrawerMenu(drawerLayout, this) { logout() }

        // ================= SLIDER =================
        setupSlider(sliderCircle, goOnlineLayout, txtOnlineStatus)

        isOnline = LocalStorage.getDriverOnlineStatus(this)

        sliderCircle.post {
            if (isOnline) {
                moveSliderOnline(sliderCircle, goOnlineLayout, txtOnlineStatus)
            } else {
                moveSliderOffline(sliderCircle, goOnlineLayout, txtOnlineStatus)
            }
        }

        // ================= PAY BUTTON (EARNING) =================
        btnPayNow.setOnClickListener {

            val earningValue =
                txtEarning.text.toString()
                    .replace("₹", "")
                    .trim()
                    .toDoubleOrNull() ?: return@setOnClickListener

            val amountToPay = abs(earningValue)

            createOrderAndStartPayment(amountToPay)
        }


        // ================= API =================
        fetchDriverEarning()
        fetchDriverVehicleInfo()
    }

    private fun createOrderAndStartPayment(amount: Double) {

        val token = LocalStorage.getToken(this) ?: return

        ApiClient.api.createOrder("Bearer $token", amount)
            .enqueue(object : retrofit2.Callback<RazorpayOrderResponse> {

                override fun onResponse(
                    call: retrofit2.Call<RazorpayOrderResponse>,
                    response: retrofit2.Response<RazorpayOrderResponse>
                ) {

                    if (response.isSuccessful && response.body() != null) {

                        val order = response.body()!!

                        startRazorpayPayment(
                            order.orderId,
                            order.amount
                        )

                    } else {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Order creation failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<RazorpayOrderResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@DashboardActivity,
                        "Network error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }


    private fun startRazorpayPayment(
        orderId: String,
        amount: Int
    ) {

        val checkout = Checkout()
        checkout.setKeyID("rzp_test_SFEc0SwSaQSBkk")

        try {

            val options = JSONObject()
            options.put("name", "Zarkit")
            options.put("description", "Driver Payment")
            options.put("currency", "INR")

            // IMPORTANT → Amount already in paisa from backend
            options.put("amount", amount)

            options.put("order_id", orderId)

            checkout.open(this, options)

        } catch (e: Exception) {
            Toast.makeText(this, "Payment Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    // ================= SLIDER =================
    private fun setupSlider(slider: View, parent: RelativeLayout, statusText: TextView) {
        var startX = 0f

        slider.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = view.x + (event.x - startX)
                    val minX = parent.paddingLeft.toFloat()
                    val maxX =
                        (parent.width - parent.paddingRight - view.width).toFloat()
                    view.x = newX.coerceIn(minX, maxX)
                    true
                }

                MotionEvent.ACTION_UP -> {

                    val sliderCenter = view.x + view.width / 2f
                    val parentCenter = parent.width / 2f

                    if (sliderCenter > parentCenter) {

                        if (blockGoOnline) {

                            // Move back
                            moveSliderOffline(view, parent, statusText)

                            Toast.makeText(
                                this,
                                "First pay pending amount to go online",
                                Toast.LENGTH_LONG
                            ).show()

                            return@setOnTouchListener true
                        }

                        moveSliderOnline(view, parent, statusText)
                        isOnline = true
                        LocalStorage.saveDriverOnlineStatus(this, true)
                        DriverStatusHelper.goOnline(this)

                        Toast.makeText(this, "You are ONLINE", Toast.LENGTH_SHORT).show()

                    } else {

                        moveSliderOffline(view, parent, statusText)
                        isOnline = false
                        LocalStorage.saveDriverOnlineStatus(this, false)
                        DriverStatusHelper.goOffline(this)

                        Toast.makeText(this, "You are OFFLINE", Toast.LENGTH_SHORT).show()
                    }

                    true
                }

                else -> false
            }
        }
    }

    // ================= UI MOVEMENT =================
    private fun moveSliderOnline(slider: View, parent: RelativeLayout, statusText: TextView) {
        val endX =
            (parent.width - parent.paddingRight - slider.width).toFloat()

        slider.animate().x(endX).setDuration(200).start()
        statusText.text = "ONLINE"
        parent.setBackgroundResource(R.drawable.btn_online)
        statusDot.setBackgroundResource(R.drawable.status_dot_green)
    }

    private fun moveSliderOffline(slider: View, parent: RelativeLayout, statusText: TextView) {
        val startX = parent.paddingLeft.toFloat()

        slider.animate().x(startX).setDuration(200).start()
        statusText.text = "GO ONLINE"
        parent.setBackgroundResource(R.drawable.btn_offline)
        statusDot.setBackgroundResource(R.drawable.status_dot_red)
    }

    override fun onStart() {
        super.onStart()
        if (LocalStorage.getToken(this).isNullOrEmpty()) logout()
    }

    // ================= EARNINGS =================
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
                    txtDriverName.text = "${data.firstName} ${data.lastName}"

                    handleEarningRules(data.earning, data.isBlocked)
                }

                override fun onFailure(call: retrofit2.Call<DriverEarningResponse>, t: Throwable) {
                    Log.e("EARNING_API", "Network error", t)
                }
            })
    }

    // ================= EARNING RULE =================
    private fun handleEarningRules(earning: Double, isBlocked: Boolean) {

        if (isBlocked) {
            btnPayNow.visibility = View.GONE
            blockGoOnline = true
            showPopup(
                "Admin Verification Pending",
                "First admin must verify your documents. Then you can pay ₹500 registration fees to start working."
            )
            return
        }

        // Show Pay button if ANY negative amount
        if (earning < 0) {
            btnPayNow.visibility = View.VISIBLE
        } else {
            btnPayNow.visibility = View.GONE
        }

        // Block only if -500 or below
        blockGoOnline = earning <= -500
    }

    // ================= VEHICLE INFO =================
    private fun fetchDriverVehicleInfo() {
        val token = LocalStorage.getToken(this)
        val driverId = LocalStorage.getUserId(this)
        if (token.isNullOrEmpty() || driverId == 0L) return

        ApiClient.api.getDriverVehicleInfo("Bearer $token", driverId)
            .enqueue(object : retrofit2.Callback<DriverVehicleInfoResponse> {

                override fun onResponse(
                    call: retrofit2.Call<DriverVehicleInfoResponse>,
                    response: retrofit2.Response<DriverVehicleInfoResponse>
                ) {
                    val data = response.body() ?: return
                    val vehicleType =
                        data.vehicleType.replace("_", " ").uppercase()

                    txtVehicleInfo.text =
                        "$vehicleType (${data.vehicleSubType}) • ${data.vehicleNumber}"
                }

                override fun onFailure(call: retrofit2.Call<DriverVehicleInfoResponse>, t: Throwable) {
                    Log.e("VEHICLE_API", "Vehicle info error", t)
                }
            })
    }

    // ================= PAYMENT VERIFY =================
    private fun verifyPaymentWithBackend(
        paymentId: String,
        orderId: String,
        signature: String
    ) {
        val token = LocalStorage.getToken(this) ?: return

        val request = RazorpayVerifyRequest(
            paymentId,
            orderId,
            signature
        )

        ApiClient.api.verifyRazorpayPayment("Bearer $token", request)
            .enqueue(object : retrofit2.Callback<Void> {

                override fun onResponse(
                    call: retrofit2.Call<Void>,
                    response: retrofit2.Response<Void>
                ) {
                    if (response.isSuccessful) {
                        blockGoOnline = false
                        btnPayNow.visibility = View.GONE
                        fetchDriverEarning()
                    } else {
                        Log.e("PAY_VERIFY", "Verify failed: ${response.code()}")
                        showPopup("Payment Error", "Verification failed. Contact support.")
                    }
                }

                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    Log.e("PAY_VERIFY", "Backend error", t)
                    showPopup("Network Error", "Unable to verify payment")
                }
            })
    }

    override fun onPaymentSuccess(
        razorpayPaymentId: String?,
        paymentData: PaymentData?
    ) {

        if (razorpayPaymentId == null || paymentData == null) {
            Toast.makeText(this, "Payment data missing", Toast.LENGTH_LONG).show()
            return
        }

        val orderId = paymentData.orderId ?: ""
        val signature = paymentData.signature ?: ""

        Log.d("RAZORPAY_DEBUG", "PaymentId: $razorpayPaymentId")
        Log.d("RAZORPAY_DEBUG", "OrderId: $orderId")
        Log.d("RAZORPAY_DEBUG", "Signature: $signature")

        verifyPaymentWithBackend(
            razorpayPaymentId,
            orderId,
            signature
        )
    }


    override fun onPaymentError(
        code: Int,
        response: String?,
        paymentData: PaymentData?
    ) {
        Toast.makeText(
            this,
            "Payment Failed",
            Toast.LENGTH_LONG
        ).show()
    }



    private fun formatAmount(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun logout() {
        LocalStorage.clearActiveRideId(this)
        LocalStorage.saveDriverOnlineStatus(this, false)
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
