package com.zarkit.zarkit_partner

import android.app.Activity
import android.widget.Toast
import com.razorpay.Checkout
import org.json.JSONObject
import kotlin.math.abs

object PaymentHelper {

    fun startRazorpayPayment(
        activity: Activity,
        commissionText: String
    ) {

        val checkout = Checkout()
        checkout.setKeyID("rzp_test_SFEc0SwSaQSBkk") // 🔥 Replace with LIVE in Production

        val commissionValue =
            abs(commissionText.replace("₹", "").toDoubleOrNull() ?: 0.0)

        val amountInPaise = (commissionValue * 100).toInt()

        try {

            val options = JSONObject().apply {
                put("name", "ZARKIT PARTNER")
                put("description", "Commission Payment")
                put("currency", "INR")
                put("amount", amountInPaise)
            }

            checkout.open(activity, options)

        } catch (e: Exception) {
            Toast.makeText(activity, "Payment error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
