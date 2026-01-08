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

    // ================= DRIVER REGISTER =================

    @Multipart
    @POST("driver/register")
    fun registerDriver(
        @Header("Authorization") authHeader: String,
        @Query("name") name: String,
        @Part photo1: MultipartBody.Part,
        @Part photo2: MultipartBody.Part,
        @Part photo3: MultipartBody.Part
    ): Call<ResponseBody>

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

    @Multipart
    @POST("driver/registerAssignedDriver")
    fun registerAssignedDriver(
        @Header("Authorization") authHeader: String,
        @Query("driveThisVehicle") driveThisVehicle: Boolean,
        @Query("driverName") driverName: String,
        @Query("driverPhone") driverPhone: String,
        @Part driverLicense: MultipartBody.Part
    ): Call<ResponseBody>

    // ================= RIDE =================

    @POST("driver/accept-ride")
    fun acceptRide(
        @Header("Authorization") authHeader: String,
        @Query("rideId") rideId: Long
    ): Call<Map<String, Any>>

    @GET("driver/ride/{rideId}")
    fun getRideDetails(
        @Header("Authorization") authHeader: String,
        @Path("rideId") rideId: Long
    ): Call<RideDetailsResponse>

    @POST("driver/ride/{rideId}/start")
    fun startTrip(
        @Header("Authorization") authHeader: String,
        @Path("rideId") rideId: Long
    ): Call<Map<String, Any>>

    @POST("driver/ride/{rideId}/end")
    fun endTrip(
        @Header("Authorization") authHeader: String,
        @Path("rideId") rideId: Long
    ): Call<Map<String, Any>>

    @POST("driver/location")
    fun sendDriverLocation(
        @Header("Authorization") token: String,
        @Body body: Map<String, Double>
    ): Call<Void>

    @POST("/api/call/ride/connect")
    fun callDriver(
        @Header("Authorization") auth: String,
        @Body body: Map<String, Long>
    ): Call<String>

    @GET("driver/driver/{driverId}/earning")
    fun getDriverEarningByDriverId(
        @Header("Authorization") authHeader: String,
        @Path("driverId") driverId: Long
    ): Call<DriverEarningResponse>
}
