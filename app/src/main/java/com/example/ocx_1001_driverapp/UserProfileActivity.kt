package com.example.ocx_1001_driverapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.DriverDetailsResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserProfileActivity : AppCompatActivity() {

    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtMobile: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val logoutButton: MaterialButton = findViewById(R.id.logoutButton)
        val backButton: ImageView = findViewById(R.id.backButton)
        val txtViewProfile: TextView? = findViewById(R.id.txtViewProfile)

        // ===== UI fields =====
        txtName = findViewById(R.id.txtDriverName)
        txtEmail = findViewById(R.id.txtDriverEmail)
        txtMobile = findViewById(R.id.txtDriverMobile)

        // ===== Fetch driver details =====
        fetchDriverDetails()

        // ===== LOGOUT =====
        logoutButton.setOnClickListener { performLogout() }

        // ===== BACK BUTTON =====
        backButton.setOnClickListener {
            finish() // Or redirect to DashboardActivity
        }

        // ===== VIEW PROFILE =====
        txtViewProfile?.setOnClickListener {
            finish() // Or redirect to DashboardActivity
        }
    }

    private fun fetchDriverDetails() {
        val token = LocalStorage.getToken(this)
        val driverId = LocalStorage.getUserId(this)

        if (token.isNullOrEmpty() || driverId == 0L) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.api.getDriverDetails("Bearer $token", driverId)
            .enqueue(object : Callback<DriverDetailsResponse> {
                override fun onResponse(
                    call: Call<DriverDetailsResponse>,
                    response: Response<DriverDetailsResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        txtName.text = "${data.firstName} ${data.lastName}"
                        txtEmail.text = data.email ?: "Not provided"
                        txtMobile.text = data.mobile
                    } else {
                        Toast.makeText(this@UserProfileActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DriverDetailsResponse>, t: Throwable) {
                    Toast.makeText(this@UserProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                    t.printStackTrace()
                }
            })
    }

    private fun performLogout() {
        LocalStorage.clearActiveRideId(this)
        LocalStorage.saveToken(this, "")
        LocalStorage.saveUserId(this, 0)

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
