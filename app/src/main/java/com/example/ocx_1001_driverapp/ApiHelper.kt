package com.example.ocx_1001_driverapp

import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.DriverEarningResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ApiHelper {

    fun fetchDriverEarnings(
        txtEarning: TextView,
        txtCommission: TextView,
        txtDriverName: TextView,
        btnPayNow: Button,
        onBlockChange: (Boolean) -> Unit,
        onLogout: () -> Unit  // Lambda for logout
    ) {
        val token = LocalStorage.getToken(btnPayNow.context)
        val driverId = LocalStorage.getUserId(btnPayNow.context)
        if (token.isNullOrEmpty() || driverId == 0L) {
            onLogout() // call logout via lambda
            return
        }

        ApiClient.api.getDriverEarningByDriverId("Bearer $token", driverId)
            .enqueue(object : Callback<DriverEarningResponse> {
                override fun onResponse(
                    call: Call<DriverEarningResponse>,
                    response: Response<DriverEarningResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        Log.e("EARNING_API", "Error code: ${response.code()}")
                        return
                    }

                    val data = response.body()!!
                    txtEarning.text = "₹${formatAmount(data.earning)}"
                    txtCommission.text = "₹${formatAmount(data.commission)}"
                    txtDriverName.text = "${data.firstName} ${data.lastName}"

                    handleEarningRules(data.earning, data.commission, btnPayNow, onBlockChange)
                }

                override fun onFailure(call: Call<DriverEarningResponse>, t: Throwable) {
                    Log.e("EARNING_API", "Network error", t)
                }
            })
    }

    private fun handleEarningRules(
        earning: Double,
        commission: Double,
        btnPayNow: Button,
        onBlockChange: (Boolean) -> Unit
    ) {
        when {
            earning == 0.0 && commission == -500.0 -> {
                btnPayNow.visibility = Button.VISIBLE
                DashboardHelper.showPopup(btnPayNow.context, "Registration Fee Pending", "Please pay registration fees.")
                onBlockChange(true)
            }
            earning != 0.0 && commission < -500.0 -> {
                btnPayNow.visibility = Button.VISIBLE
                DashboardHelper.showPopup(btnPayNow.context, "Commission Due", "Please clear your commission.")
                onBlockChange(true)
            }
            else -> {
                btnPayNow.visibility = Button.GONE
                onBlockChange(false)
            }
        }
    }

    private fun formatAmount(value: Double): String = String.format("%.2f", value)
}
