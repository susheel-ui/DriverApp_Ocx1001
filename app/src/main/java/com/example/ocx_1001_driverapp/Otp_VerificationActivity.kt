package com.example.ocx_1001_driverapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.SaveTokenBody
import com.example.ocx_1001_driverapp.api.VerifyOtpBody
import com.example.ocx_1001_driverapp.databinding.ActivityOtpVerificationBinding
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Otp_VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val phone = LocalStorage.getPhone(this)

        if (phone.isNullOrEmpty()) {
            Toast.makeText(this, "Phone missing. Login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.verifyButton.setOnClickListener {
            val otp = binding.otpEditText.text.toString().trim()

            if (otp.isEmpty()) {
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(phone, otp)
        }
    }

    private fun verifyOtp(phone: String, otp: String) {

        val body = VerifyOtpBody(phone, otp)

        ApiClient.api.verifyOtp(body).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                if (!response.isSuccessful) {
                    Toast.makeText(this@Otp_VerificationActivity, "Invalid OTP", Toast.LENGTH_LONG).show()
                    return
                }

                val bodyStr = response.body()?.string() ?: "{}"
                val json = JSONObject(bodyStr)

                val code = json.optString("code")
                val jwt = json.optString("token")
                val role = json.optString("role")
                val userId = json.optLong("userId")

                if (code == "LOGIN_SUCCESS") {

                    // Save locally
                    LocalStorage.saveToken(this@Otp_VerificationActivity, jwt)
                    LocalStorage.saveRole(this@Otp_VerificationActivity, role)
                    LocalStorage.saveUserId(this@Otp_VerificationActivity, userId)

                    Toast.makeText(this@Otp_VerificationActivity, "OTP Verified", Toast.LENGTH_SHORT).show()

                    // Redirect first
                    startActivity(Intent(this@Otp_VerificationActivity, DashboardActivity::class.java))
                    finish()

                    // Save FCM token
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { fcm ->

                            // If same FCM, skip API call
                            if (LocalStorage.getFcmToken(this@Otp_VerificationActivity) == fcm) return@addOnSuccessListener

                            // Save locally
                            LocalStorage.saveFcmToken(this@Otp_VerificationActivity, fcm)

                            val tokenBody = SaveTokenBody(
                                driverId = userId,
                                token = fcm
                            )

                            ApiClient.api.saveDriverToken(tokenBody)
                                .enqueue(object : Callback<ResponseBody> {

                                    override fun onResponse(
                                        call: Call<ResponseBody>,
                                        response: Response<ResponseBody>
                                    ) {
                                        println("🔥 FCM Token stored on server")
                                    }

                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                        println("❌ Failed to save FCM: ${t.message}")
                                    }
                                })
                        }
                        .addOnFailureListener {
                            println("❌ FCM token fetch failed: $it")
                        }

                } else {
                    Toast.makeText(this@Otp_VerificationActivity, "OTP Invalid", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@Otp_VerificationActivity,
                    "Network Error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
