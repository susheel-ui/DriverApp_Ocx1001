package com.zarkit.zarkit_partner

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.LoginBody
import com.zarkit.zarkit_partner.api.SaveTokenBody
import com.zarkit.zarkit_partner.databinding.ActivityLoginBinding
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        setupLoginButtonState()
        setupLoginClick()
        setupTermsCheckbox()
    }

    private fun setupTermsCheckbox() {

        val text = "I have read and agreed to Terms & Conditions and Privacy Policy"

        val spannable = SpannableString(text)

        val termsStart = text.indexOf("Terms & Conditions")
        val termsEnd = termsStart + "Terms & Conditions".length

        val privacyStart = text.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        // Terms Click
        val termsClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openLink("http://72.60.200.11/pdfs/terms_and_conditions_for_zarkit.pdf")
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.color = Color.parseColor("#1E88E5") // Blue clickable
                ds.isUnderlineText = false
            }
        }

        // Privacy Click
        val privacyClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openLink("http://72.60.200.11/pdfs/Privacy%20Policy%20for%20Zarkit.pdf")
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.color = Color.parseColor("#1E88E5")
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(termsClick, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(privacyClick, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.checkboxTerms.text = spannable
        binding.checkboxTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.checkboxTerms.highlightColor = Color.TRANSPARENT
    }

    private fun openLink(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }


    // ✅ Enable Login Button only if checkboxes checked
    private fun setupLoginButtonState() {

        binding.buttonLogin.isEnabled = false
        binding.buttonLogin.alpha = 0.5f

        fun updateState() {
            val enabled =
                binding.checkboxTerms.isChecked &&
                        binding.checkboxTds.isChecked

            binding.buttonLogin.isEnabled = enabled
            binding.buttonLogin.alpha = if (enabled) 1f else 0.5f
        }

        binding.checkboxTerms.setOnCheckedChangeListener { _, _ -> updateState() }
        binding.checkboxTds.setOnCheckedChangeListener { _, _ -> updateState() }
    }

    // ✅ Login Click
    private fun setupLoginClick() {

        binding.buttonLogin.setOnClickListener {

            val phone = binding.phoneEditText.text.toString().trim()

            if (phone.length != 10) {
                Toast.makeText(this, "Enter valid 10-digit number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            LocalStorage.savePhone(this, phone)

            getSharedPreferences("auth_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_registered", false)
                .apply()

            callLoginApi(phone)
        }
    }

    // ✅ Login API using Retrofit
    private fun callLoginApi(phone: String) {

        val body = LoginBody(phone)

        ApiClient.api.login(body).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                try {

                    // ✅ Read success OR error body
                    val raw = if (response.isSuccessful) {
                        response.body()?.string()
                    } else {
                        response.errorBody()?.string()
                    } ?: "{}"

                    val json = JSONObject(raw)
                    val code = json.optString("code")

                    when (code) {

                        // ✅ OTP Case
                        "OTP_SENT" -> handleOtpSent(json)

                        // ✅ NEED REGISTER Case
                        "NEED_REGISTER" -> {
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    ResistrationDriver::class.java
                                )
                            )
                            finish()
                        }

                        else -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "Response: $code",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@LoginActivity,
                        "Parsing Error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@LoginActivity,
                    "Network Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    //  OTP Flow
    private fun handleOtpSent(json: JSONObject) {

        val userId = json.optLong("userId", 0)

        if (userId != 0L) {
            LocalStorage.saveUserId(this, userId)
        }

        uploadTokenIfExists()

        startActivity(
            Intent(this, Otp_VerificationActivity::class.java)
        )
    }

    // ✅ Upload FCM Token
    private fun uploadTokenIfExists() {

        val token = LocalStorage.getFcmToken(this)
        val userId = LocalStorage.getUserId(this)

        if (token == null || userId == 0L) return

        val body = SaveTokenBody(
            driverId = userId,
            token = token
        )

        val auth = "Bearer ${LocalStorage.getToken(this) ?: ""}"

        ApiClient.api.saveDriverToken(auth, body)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    println("Token uploaded: ${response.code()}")
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    println("Token upload failed")
                }
            })
    }
}
