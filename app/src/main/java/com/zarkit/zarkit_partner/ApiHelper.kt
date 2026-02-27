package com.zarkit.zarkit_partner

import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.DriverEarningResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ApiHelper {

    fun fetchDriverEarnings(
        txtEarning: TextView,
        txtDriverName: TextView,
        btnPayNow: Button,
        onBlockChange: (Boolean) -> Unit,
        onLogout: () -> Unit
    ) {
        val context = btnPayNow.context
        val token = LocalStorage.getToken(context)
        val driverId = LocalStorage.getUserId(context)

        if (token.isNullOrEmpty() || driverId == 0L) {
            onLogout()
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
                    txtDriverName.text = "${data.firstName} ${data.lastName}"

                    handleEarningRules(
                        earning = data.earning,
                        btnPayNow = btnPayNow,
                        onBlockChange = onBlockChange
                    )
                }

                override fun onFailure(call: Call<DriverEarningResponse>, t: Throwable) {
                    Log.e("EARNING_API", "Network error", t)
                }
            })
    }

    private fun handleEarningRules(
        earning: Double,
        btnPayNow: Button,
        onBlockChange: (Boolean) -> Unit
    ) {
        if (earning < 0) {
            btnPayNow.visibility = Button.VISIBLE
            DashboardHelper.showPopup(
                btnPayNow.context,
                "Payment Pending",
                "Please clear your pending amount to go online."
            )
            onBlockChange(true)
        } else {
            btnPayNow.visibility = Button.GONE
            onBlockChange(false)
        }
    }

    private fun formatAmount(value: Double): String =
        String.format("%.2f", value)
}
