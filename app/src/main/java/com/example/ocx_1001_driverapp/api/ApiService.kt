package com.example.ocx_1001_driverapp.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("/auth/verify")
    fun verifyOtp(@Body body: VerifyOtpBody): Call<ResponseBody>

    @POST("/api/driver/save-token")
    fun saveDriverToken(
        @Header("Authorization") authHeader: String,
        @Body body: SaveTokenBody
    ): Call<ResponseBody>
}
