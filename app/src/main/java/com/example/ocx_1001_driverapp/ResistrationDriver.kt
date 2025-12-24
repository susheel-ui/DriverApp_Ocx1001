package com.example.ocx_1001_driverapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.databinding.ActivityResistrationDriverBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ResistrationDriver : AppCompatActivity() {

    private lateinit var binding: ActivityResistrationDriverBinding
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResistrationDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonNext.setOnClickListener {

            val mobile = LocalStorage.getPhone(this)

            if (mobile.isNullOrEmpty()) {
                Toast.makeText(this, "Mobile not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firstName = binding.firstNameEditText.text.toString().trim()
            val lastName = binding.lastNameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()

            if (firstName.isEmpty()) {
                Toast.makeText(this, "Enter first name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = """
                {
                  "mobile": "$mobile",
                  "firstName": "$firstName",
                  "lastName": "$lastName",
                  "email": "$email",
                  "role": "DRIVER"
                }
            """.trimIndent()

            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("http://172.20.10.2:8080/auth/register")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ResistrationDriver,
                            "Network Error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {

                    val raw = response.body?.string() ?: "{}"
                    val jsonResp = JSONObject(raw)
                    val code = jsonResp.optString("code")

                    runOnUiThread {
                        if (response.isSuccessful && code == "REGISTER_OTP_SENT") {

                            val userId = jsonResp.optLong("userId", 0)
                            if (userId != 0L) {
                                LocalStorage.saveUserId(this@ResistrationDriver, userId)
                            }

                            startActivity(
                                Intent(
                                    this@ResistrationDriver,
                                    Otp_VerificationActivity::class.java
                                )
                            )
                        } else {
                            Toast.makeText(
                                this@ResistrationDriver,
                                "Register failed: $code",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        }
    }
}
