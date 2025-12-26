package com.example.ocx_1001_driverapp.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ================= AUTH =================

    @POST("auth/verify")
    fun verifyOtp(
        @Body body: VerifyOtpBody
    ): Call<ResponseBody>

    @POST("api/driver/save-token")
    fun saveDriverToken(
        @Header("Authorization") authHeader: String,
        @Body body: SaveTokenBody
    ): Call<ResponseBody>

    // ================= Owner REGISTER =================
    // (WORKING – DO NOT CHANGE)

    @Multipart
    @POST("driver/register")
    fun registerDriver(
        @Header("Authorization") authHeader: String,

        @Query("name") name: String,

        @Part photo1: MultipartBody.Part,
        @Part photo2: MultipartBody.Part,
        @Part photo3: MultipartBody.Part
    ): Call<ResponseBody>

    // ================= VEHICLE REGISTER =================
    // (FIXED – SAME STYLE AS DRIVER)

    @Multipart
    @POST("driver/registerVehicle")
    fun registerVehicle(
        @Header("Authorization") authHeader: String,

        @Query("vehicleNumber") vehicleNumber: String,
        @Query("city") city: String,
        @Query("vehicleType") vehicleType: String,
        @Query("vehicleSubType") vehicleSubType: String,

        @Part vehicleImage: MultipartBody.Part
    ): Call<ResponseBody>

    //================= Driver REGISTER =================

    @Multipart
    @POST("driver/registerAssignedDriver")
    fun registerAssignedDriver(
        @Header("Authorization") authHeader: String,

        @Query("driveThisVehicle") driveThisVehicle: Boolean,
        @Query("driverName") driverName: String,
        @Query("driverPhone") driverPhone: String,

        @Part driverLicense: MultipartBody.Part
    ): Call<ResponseBody>

    @POST("/driver/accept-ride")
    fun acceptRide(
        @Query("rideId") rideId: Long
    ): Call<Map<String, Any>>


}
