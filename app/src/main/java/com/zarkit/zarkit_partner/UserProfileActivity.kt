package com.zarkit.zarkit_partner

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zarkit.zarkit_partner.api.*
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class UserProfileActivity : AppCompatActivity() {

    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtMobile: TextView
    private lateinit var txtTotalEarning: TextView
    private lateinit var txtAccountNumber: TextView
    private lateinit var txtIfsc: TextView
    private lateinit var txtBankStatus: TextView
    private lateinit var btnWithdraw: MaterialButton
    private lateinit var btnEditBank: MaterialButton

    private var totalEarningAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootContent)) { view, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        val logoutButton: MaterialButton = findViewById(R.id.logoutButton)
        val backButton: ImageView = findViewById(R.id.backButton)

        txtName = findViewById(R.id.txtDriverName)
        txtEmail = findViewById(R.id.txtDriverEmail)
        txtMobile = findViewById(R.id.txtDriverMobile)
        txtTotalEarning = findViewById(R.id.txtTotalEarning)
        txtAccountNumber = findViewById(R.id.txtAccountNumber)
        txtIfsc = findViewById(R.id.txtIfsc)
        txtBankStatus = findViewById(R.id.txtBankStatus)
        btnWithdraw = findViewById(R.id.btnWithdraw)
        btnEditBank = findViewById(R.id.btnEditBank)

        fetchDriverDetails()
        fetchBankDetails()

        logoutButton.setOnClickListener { performLogout() }

        backButton.setOnClickListener {
            val intent = Intent(this@UserProfileActivity, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        btnWithdraw.setOnClickListener { openWithdrawDialog() }
        btnEditBank.setOnClickListener { openEditBankDialog() }
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

                        totalEarningAmount = data.earning
                        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                        txtTotalEarning.text = formatter.format(totalEarningAmount)
                    } else {
                        Toast.makeText(this@UserProfileActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DriverDetailsResponse>, t: Throwable) {
                    Toast.makeText(this@UserProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchBankDetails() {
        val token = LocalStorage.getToken(this) ?: return
        val driverId = LocalStorage.getUserId(this)
        ApiClient.api.getBankDetails("Bearer $token", driverId)
            .enqueue(object : Callback<DriverBankDetailsResponse> {
                override fun onResponse(
                    call: Call<DriverBankDetailsResponse>,
                    response: Response<DriverBankDetailsResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        txtAccountNumber.text = data.accountNumber ?: "Not available"
                        txtIfsc.text = data.ifscCode ?: "Not available"
                        txtBankStatus.text = if (data.accountNumber != null && data.ifscCode != null) "Bank Verified" else "Bank Not Added"
                    }
                }

                override fun onFailure(call: Call<DriverBankDetailsResponse>, t: Throwable) {
                    Toast.makeText(this@UserProfileActivity, "Failed to fetch bank details", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ================= WITHDRAW POPUP =================
    private fun openWithdrawDialog() {
        if (totalEarningAmount < 500) {
            Toast.makeText(this, "Minimum ₹500 required to withdraw", Toast.LENGTH_LONG).show()
            return
        }

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_withdraw, null)
        bottomSheetDialog.setContentView(view)

        val edtAmount = view.findViewById<EditText>(R.id.edtAmount)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitWithdraw)

        val txtDialogAccount = view.findViewById<TextView>(R.id.txtDialogAccountNumber)
        val txtDialogIfsc = view.findViewById<TextView>(R.id.txtDialogIfsc)

        // Show existing bank details
        txtDialogAccount.text = "Account: ${txtAccountNumber.text}"
        txtDialogIfsc.text = "IFSC: ${txtIfsc.text}"

        btnSubmit.setOnClickListener {
            val enteredAmount = edtAmount.text.toString().toDoubleOrNull()
            if (enteredAmount == null) {
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (enteredAmount < 500) {
                Toast.makeText(this, "Minimum withdraw amount is ₹500", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (enteredAmount > totalEarningAmount) {
                Toast.makeText(this, "Amount exceeds available balance", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val token = LocalStorage.getToken(this) ?: ""
            val driverId = LocalStorage.getUserId(this)
            val request = WithdrawalRequest(driverId, enteredAmount)

            ApiClient.api.requestWithdrawal("Bearer $token", request)
                .enqueue(object : Callback<WithdrawalResponse> {
                    override fun onResponse(call: Call<WithdrawalResponse>, response: Response<WithdrawalResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val data = response.body()!!
                            Toast.makeText(this@UserProfileActivity, data.message, Toast.LENGTH_LONG).show()
                            if (data.status == "OK") {
                                totalEarningAmount -= enteredAmount
                                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                                txtTotalEarning.text = formatter.format(totalEarningAmount)
                                bottomSheetDialog.dismiss()
                            }
                        } else {
                            Toast.makeText(this@UserProfileActivity, "Failed to request withdrawal", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<WithdrawalResponse>, t: Throwable) {
                        Toast.makeText(this@UserProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        bottomSheetDialog.show()
    }


    // ================= EDIT BANK DETAILS POPUP =================
    private fun openEditBankDialog() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_bank, null)
        bottomSheetDialog.setContentView(view)

        val edtAccount = view.findViewById<EditText>(R.id.edtAccountNumber)
        val edtIfsc = view.findViewById<EditText>(R.id.edtIfscCode)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitBank)

        // Pre-fill existing details
        edtAccount.setText(txtAccountNumber.text.toString().takeIf { it != "Not available" } ?: "")
        edtIfsc.setText(txtIfsc.text.toString().takeIf { it != "Not available" } ?: "")

        btnSubmit.setOnClickListener {
            val accountNumber = edtAccount.text.toString().trim()
            val ifscCode = edtIfsc.text.toString().trim()

            if (accountNumber.isEmpty() || ifscCode.isEmpty()) {
                Toast.makeText(this, "Enter valid bank details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = LocalStorage.getToken(this) ?: ""
            val driverId = LocalStorage.getUserId(this)
            val request = UpdateBankDetailsRequest(driverId, accountNumber, ifscCode)

            ApiClient.api.updateBankDetails("Bearer $token", request)
                .enqueue(object : Callback<UpdateBankDetailsResponse> {
                    override fun onResponse(
                        call: Call<UpdateBankDetailsResponse>,
                        response: Response<UpdateBankDetailsResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val data = response.body()!!
                            Toast.makeText(this@UserProfileActivity, data.message, Toast.LENGTH_LONG).show()
                            if (data.status == "OK") {
                                txtAccountNumber.text = accountNumber
                                txtIfsc.text = ifscCode
                                txtBankStatus.text = "Bank Verified"
                                bottomSheetDialog.dismiss()
                            }
                        } else {
                            Toast.makeText(this@UserProfileActivity, "Failed to update bank details", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UpdateBankDetailsResponse>, t: Throwable) {
                        Toast.makeText(this@UserProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        bottomSheetDialog.show()
    }

    private fun performLogout() {
        LocalStorage.clearActiveRideId(this)
        LocalStorage.saveToken(this, "")
        LocalStorage.saveUserId(this, 0)

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
