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

        // Initially disable login button
        binding.buttonLogin.isEnabled = false
        binding.buttonLogin.alpha = 0.5f // visually disabled

        // Function to check checkboxes
        fun updateLoginButtonState() {
            val allChecked = binding.checkboxTerms.isChecked && binding.checkboxTds.isChecked
            binding.buttonLogin.isEnabled = allChecked
            binding.buttonLogin.alpha = if (allChecked) 1f else 0.5f
        }

        // Listen for checkbox changes
        binding.checkboxTerms.setOnCheckedChangeListener { _, _ -> updateLoginButtonState() }
        binding.checkboxTds.setOnCheckedChangeListener { _, _ -> updateLoginButtonState() }

        // Login click
        binding.buttonLogin.setOnClickListener {
            val phone = binding.phoneEditText.text.toString().trim()

            if (phone.length != 10) {
                Toast.makeText(this, "Enter valid 10-digit number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save phone locally
            LocalStorage.savePhone(this, phone)

            // Prevent auto dashboard redirect
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
            .url("http://72.60.200.11:8080/auth/login")
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
                            // Save userId
                            val userId = json.optLong("userId", 0)
                            if (userId != 0L) {
                                LocalStorage.saveUserId(this@LoginActivity, userId)
                                println("Saved userId = $userId")
                            }

                            // Upload token immediately
                            uploadExistingToken()

                            // Navigate to OTP screen
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
            .url("http://72.60.200.11:8080/api/driver/save-token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ Token upload failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                println("✅ Token uploaded: ${response.code}")
            }
        })
    }
}
