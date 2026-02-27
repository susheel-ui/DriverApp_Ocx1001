package com.zarkit.zarkit_partner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.SaveTokenBody
import com.zarkit.zarkit_partner.api.VerifyOtpBody
import com.zarkit.zarkit_partner.databinding.ActivityOtpVerificationBinding
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
        enableEdgeToEdge()
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val phone = LocalStorage.getPhone(this)

        if (phone.isNullOrEmpty()) {
            Toast.makeText(this, "Phone missing. Login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ SHOW PHONE NUMBER
        binding.phoneNumberTextView.text = phone

        // ✅ CHANGE NUMBER → LOGIN SCREEN
        binding.changeNumberTextView.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
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
                    Toast.makeText(
                        this@Otp_VerificationActivity,
                        "Invalid OTP",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val bodyStr = response.body()?.string() ?: "{}"
                val json = JSONObject(bodyStr)

                val code = json.optString("code")
                val token = json.optString("token")
                val role = json.optString("role")
                val userId = json.optLong("userId")
                val isRegistered = json.optBoolean("is_registered", true)

                if (code == "LOGIN_SUCCESS") {

                    if (role != "DRIVER") {
                        Toast.makeText(
                            this@Otp_VerificationActivity,
                            "Only drivers are allowed to login",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    LocalStorage.saveToken(this@Otp_VerificationActivity, token)
                    LocalStorage.saveRole(this@Otp_VerificationActivity, role)
                    LocalStorage.saveUserId(this@Otp_VerificationActivity, userId)
                    LocalStorage.saveIsRegistered(this@Otp_VerificationActivity, isRegistered)

                    Toast.makeText(
                        this@Otp_VerificationActivity,
                        "OTP Verified",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (!isRegistered) {
                        startActivity(
                            Intent(
                                this@Otp_VerificationActivity,
                                RegistrationActivity::class.java
                            )
                        )
                    } else {
                        startActivity(
                            Intent(
                                this@Otp_VerificationActivity,
                                DashboardActivity::class.java
                            )
                        )
                    }

                    finish()
                    saveFcmTokenToServer(userId)

                } else {
                    Toast.makeText(
                        this@Otp_VerificationActivity,
                        "OTP Invalid",
                        Toast.LENGTH_LONG
                    ).show()
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

    private fun saveFcmTokenToServer(userId: Long) {

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcm ->

                val oldToken = LocalStorage.getFcmToken(this)
                val jwt = LocalStorage.getToken(this)

                if (jwt.isNullOrEmpty()) return@addOnSuccessListener

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
                            LocalStorage.saveFcmToken(this@Otp_VerificationActivity, fcm)
                        } else {
                            LocalStorage.saveFcmToken(this@Otp_VerificationActivity, "")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        LocalStorage.saveFcmToken(this@Otp_VerificationActivity, "")
                    }
                })
            }
    }
}
