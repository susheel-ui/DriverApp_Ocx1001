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

            callLoginApi(phone)
        }
    }

    private fun callLoginApi(phone: String) {

        val json = """{"mobile":"$phone"}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:8080/auth/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Network Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val raw = response.body?.string()
                println("🚀 RAW RESPONSE = $raw")

                var jsonObj = JSONObject()
                if (!raw.isNullOrEmpty()) jsonObj = JSONObject(raw)

                val codeValue = jsonObj.optString("code", "").uppercase()

                runOnUiThread {

                    when {
                        // ---------------------------------
                        // OTP Flow → Go to OTP screen
                        // ---------------------------------
                        response.code == 200 && codeValue.contains("OTP") -> {

                            LocalStorage.savePhone(this@LoginActivity, phone)

                            startActivity(
                                Intent(this@LoginActivity, Otp_VerificationActivity::class.java)
                            )
                        }

                        // ---------------------------------
                        // Registration Flow → Go to Register
                        // ---------------------------------
                        response.code == 404 && codeValue.contains("REGISTER") -> {

                            LocalStorage.savePhone(this@LoginActivity, phone)

                            startActivity(
                                Intent(this@LoginActivity, RegistrationActivity::class.java)
                            )
                        }

                        else -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "Unexpected Server Response",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }
}
