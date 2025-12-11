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
                    Toast.makeText(this@Otp_VerificationActivity, "Invalid OTP", Toast.LENGTH_LONG)
                        .show()
                    return
                }

                val bodyStr = response.body()?.string() ?: "{}"
                val json = JSONObject(bodyStr)

                val code = json.optString("code")
                val token = json.optString("token")
                val role = json.optString("role")
                val userId = json.optLong("userId")

                if (code == "LOGIN_SUCCESS") {

                    // Save login details
                    LocalStorage.saveToken(this@Otp_VerificationActivity, token)
                    LocalStorage.saveRole(this@Otp_VerificationActivity, role)
                    LocalStorage.saveUserId(this@Otp_VerificationActivity, userId)

                    Toast.makeText(
                        this@Otp_VerificationActivity,
                        "OTP Verified",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Redirect
                    startActivity(
                        Intent(
                            this@Otp_VerificationActivity,
                            DashboardActivity::class.java
                        )
                    )
                    finish()

                    // Save FCM Token
                    saveFcmTokenToServer(userId)

                } else {
                    Toast.makeText(this@Otp_VerificationActivity, "OTP Invalid", Toast.LENGTH_LONG)
                        .show()
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

    //code for production beacuse if fcm token present it will skip api call and if not it will call
//    private fun saveFcmTokenToServer(userId: Long) {
//
//        FirebaseMessaging.getInstance().token
//            .addOnSuccessListener { fcm ->
//
//                val oldToken = LocalStorage.getFcmToken(this)
//
//                if (oldToken == fcm) {
//                    println("⚠️ FCM unchanged — skipping API call")
//                    return@addOnSuccessListener
//                }
//
//                LocalStorage.saveFcmToken(this, fcm)
//
//                val jwt = LocalStorage.getToken(this)
//
//                val tokenBody = SaveTokenBody(
//                    driverId = userId,
//                    token = fcm
//                )
//
//                ApiClient.api.saveDriverToken(
//                    authHeader = "Bearer $jwt",
//                    body = tokenBody
//                ).enqueue(object : Callback<ResponseBody> {
//
//                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                        if (response.isSuccessful) {
//                            println("🔥 FCM Token saved on server successfully")
//                        } else {
//                            println("❌ Server rejected FCM Token: ${response.code()}")
//                        }
//                    }
//
//                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                        println("❌ Network error saving FCM token: ${t.message}")
//                    }
//                })
//            }
//            .addOnFailureListener {
//                println("❌ Failed to fetch FCM token: $it")
//            }
//    }
//}


    private fun saveFcmTokenToServer(userId: Long) {

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcm ->

                val oldToken = LocalStorage.getFcmToken(this)
                val jwt = LocalStorage.getToken(this)

                if (jwt.isNullOrEmpty()) {
                    println("❌ No JWT found. Cannot send FCM to backend.")
                    return@addOnSuccessListener
                }

                // If same token, still send (backend may want update)
                if (oldToken == fcm) {
                    println("⚠️ FCM same but sending again to backend")
                }

                val tokenBody = SaveTokenBody(
                    driverId = userId,
                    token = fcm
                )

                ApiClient.api.saveDriverToken(
                    authHeader = "Bearer $jwt",
                    body = tokenBody
                ).enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            println("🔥 FCM updated on server successfully")
                            LocalStorage.saveFcmToken(this@Otp_VerificationActivity, fcm)
                        } else {
                            println("❌ Server rejected FCM Token: ${response.code()} — deleting local token")
                            LocalStorage.saveFcmToken(
                                this@Otp_VerificationActivity,
                                ""
                            ) // delete local
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        println("❌ Failed to send token: ${t.message} → deleting local token")
                        LocalStorage.saveFcmToken(this@Otp_VerificationActivity, "")
                    }
                })
            }
            .addOnFailureListener {
                println("❌ Could not fetch FCM token: $it")
            }
    }
}
