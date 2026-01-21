package com.example.ocx_1001_driverapp

import android.content.Context
import android.widget.Toast
import com.razorpay.Checkout
import org.json.JSONObject
import kotlin.math.abs

object PaymentHelper {

    fun startRazorpayPayment(context: Context, commissionText: String, onPaymentSuccess: (paymentId: String) -> Unit) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_S5hfApqAqe4arW") // replace with your key

        val commissionValue = abs(commissionText.replace("₹", "").toDouble())
        val amountInPaise = (commissionValue * 100).toInt()

        try {
            val options = JSONObject().apply {
                put("name", "ZARKIT PARTNER")
                put("description", "Commission Payment")
                put("currency", "INR")
                put("amount", amountInPaise)
                put("payment_capture", 1)
            }

            checkout.open(context as DashboardActivity, options)

        } catch (e: Exception) {
            Toast.makeText(context, "Payment error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
