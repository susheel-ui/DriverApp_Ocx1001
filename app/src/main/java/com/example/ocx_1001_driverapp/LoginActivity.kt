package com.example.ocx_1001_driverapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.databinding.ActivityLoginBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            val phone = binding.phoneEditText.text.toString().trim()

            if (phone.length != 10) {
                Toast.makeText(this, "Enter valid 10-digit number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save phone
            LocalStorage.savePhone(this, phone)

            // 🔴 ADDED: mark login started (prevents auto dashboard redirect)
            getSharedPreferences("auth_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_registered", false)
                .apply()

            callLoginApi(phone)
        }
    }

    private fun callLoginApi(phone: String) {

        val json = """{"mobile":"$phone"}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.29.149:8080/auth/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Network Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val raw = response.body?.string() ?: "{}"
                val json = JSONObject(raw)
                val code = json.optString("code")

                runOnUiThread {

                    when (code) {

                        "OTP_SENT" -> {
                            // Save userId (important)
                            val userId = json.optLong("userId", 0)
                            if (userId != 0L) {
                                LocalStorage.saveUserId(this@LoginActivity, userId)
                                println("Saved userId = $userId")
                            }

                            // Upload token immediately if available
                            uploadExistingToken()

                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    Otp_VerificationActivity::class.java
                                )
                            )
                        }

                        "NEED_REGISTER" -> {
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    ResistrationDriver::class.java
                                )
                            )
                        }

                        else -> Toast.makeText(
                            this@LoginActivity,
                            "Unexpected: $code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun uploadExistingToken() {
        val token = LocalStorage.getFcmToken(this)
        val userId = LocalStorage.getUserId(this)

        if (token == null || userId == 0L) {
            println("⚠ Cannot upload token — token or userId missing!")
            return
        }

        val json = """{"driverId":$userId,"token":"$token"}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.29.149:8080/api/driver/save-token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ Token upload failed in LoginActivity: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                println("✅ Token uploaded after login: ${response.code}")
            }
        })
    }
}
