package com.zarkit.zarkit_partner

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.zarkit.zarkit_partner.api.ApiClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object DriverStatusHelper {

    fun goOnline(context: Context) {
        val token = LocalStorage.getToken(context)
        val driverId = LocalStorage.getUserId(context)
        if (token.isNullOrEmpty() || driverId == 0L) return

        ApiClient.api.driverOnline("Bearer $token", driverId)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    // 🔥 SAVE ONLINE STATUS LOCALLY
                    LocalStorage.saveDriverOnlineStatus(context, true)

                    Log.d(
                        "DRIVER_STATUS",
                        "ONLINE: ${response.body()?.string()}"
                    )
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("DRIVER_STATUS", "Online API error", t)
                    Toast.makeText(
                        context,
                        "Failed to go online",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    fun goOffline(context: Context) {
        val token = LocalStorage.getToken(context)
        val driverId = LocalStorage.getUserId(context)
        if (token.isNullOrEmpty() || driverId == 0L) return

        ApiClient.api.driverOffline("Bearer $token", driverId)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    // 🔥 SAVE OFFLINE STATUS LOCALLY
                    LocalStorage.saveDriverOnlineStatus(context, false)

                    Log.d(
                        "DRIVER_STATUS",
                        "OFFLINE: ${response.body()?.string()}"
                    )
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("DRIVER_STATUS", "Offline API error", t)
                    Toast.makeText(
                        context,
                        "Failed to go offline",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
