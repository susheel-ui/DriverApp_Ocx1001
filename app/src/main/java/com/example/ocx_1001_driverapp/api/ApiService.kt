package com.example.ocx_1001_driverapp.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ✅ VERIFY OTP
    @POST("/auth/verify")
    fun verifyOtp(
        @Body body: VerifyOtpBody
    ): Call<ResponseBody>

    // ✅ SAVE FCM TOKEN
    @POST("/api/driver/save-token")
    fun saveDriverToken(
        @Header("Authorization") authHeader: String,
        @Body body: SaveTokenBody
    ): Call<ResponseBody>

    // ✅ DRIVER REGISTER (MULTIPART)
    @Multipart
    @POST("/driver/register")
    fun registerDriver(
        @Header("Authorization") authHeader: String,

        // ✅ QUERY PARAM
        @Query("name") name: String,

        // ✅ FILE PARTS
        @Part photo1: MultipartBody.Part,
        @Part photo2: MultipartBody.Part,
        @Part photo3: MultipartBody.Part
    ): Call<ResponseBody>

}
