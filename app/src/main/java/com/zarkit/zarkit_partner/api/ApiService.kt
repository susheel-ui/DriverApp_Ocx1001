package com.zarkit.zarkit_partner.api

import com.zarkit.zarkit_partner.api.Entites.body
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

    @GET("driver/{id}")
    fun getDriverDetails(
        @Header("Authorization") token: String,
        @Path("id") driverId: Long
    ): retrofit2.Call<DriverDetailsResponse>

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
        @Part photo3: MultipartBody.Part,
        @Part photo4: MultipartBody.Part
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

    @GET("driver/driver/{driverId}/earning")
    fun getDriverEarningByDriverId(
        @Header("Authorization") authHeader: String,
        @Path("driverId") driverId: Long
    ): Call<DriverEarningResponse>

    @POST("driver/location")
    fun sendDriverLocation(
        @Header("Authorization") token: String,
        @Body body: Map<String, Double>
    ): Call<Void>

    @POST("api/call/ride/connect")
    fun callRideConnect(
        @Header("Authorization") token: String,
        @Body request: CallRideRequest
    ): Call<String>

    @POST("driver/payment/verify")
    fun verifyRazorpayPayment(
        @Header("Authorization") token: String,
        @Body body: RazorpayVerifyRequest
    ): Call<Void>

    @POST("driver/payment/create-order")
    fun createOrder(
        @Header("Authorization") token: String,
        @Query("amount") amount: Double
    ): Call<RazorpayOrderResponse>

    @POST("driver/collect-payment-cash")
    fun collectCashPayment(
        @Header("Authorization") token: String,
        @Body request: body
    ): Call<ResponseBody>

    @GET("driver/latest/fare")
    fun getLatestFare(
        @Header("Authorization") authHeader: String,
        @Query("driverId") driverId: Long
    ): Call<FareResponse>

    @POST("driver/online/{id}")
    fun goOnline(
        @Header("Authorization") token: String,
        @Path("id") driverId: Long
    ): Call<String>


    @POST("driver/offline/{id}")
    fun goOffline(
        @Header("Authorization") token: String,
        @Path("id") driverId: Long
    ): Call<String>

    @GET("driver/all-booking/{driverId}")
    fun getAllTrips(
        @Path("driverId") driverId: Int,
        @Header("Authorization") token: String
    ): Call<List<Trip>>

    @POST("driver/online/{driverId}")
    fun driverOnline(
        @Header("Authorization") token: String,
        @Path("driverId") driverId: Long
    ): Call<ResponseBody>

    @POST("driver/offline/{driverId}")
    fun driverOffline(
        @Header("Authorization") token: String,
        @Path("driverId") driverId: Long
    ): Call<ResponseBody>

    @GET("driver/driver-vehicle-info/{driverId}")
    fun getDriverVehicleInfo(
        @Header("Authorization") token: String,
        @Path("driverId") driverId: Long
    ): Call<DriverVehicleInfoResponse>

    @POST("driver/create-qr")
    fun createQr(
        @Header("Authorization") token: String,
        @Body request: CreateQrRequest
    ): Call<QrResponse>

    @GET("driver/payment-status/{id}")
    fun getPaymentStatus(
        @Header("Authorization") token: String,
        @Path("id") transactionId: Long
    ): Call<PaymentStatusResponse>

    @GET("driver/paymentpaid-status/{rideId}")
    fun checkPaymentStatus(
        @Header("Authorization") token: String,
        @Path("rideId") rideId: Long
    ): Call<Boolean>

    @POST("driver/collect-payment-online")
    fun collectOnlinePayment(
        @Header("Authorization") token: String,
        @Body request: body
    ): Call<Void>

    @POST("driver/withdrawal-request")
    fun requestWithdrawal(
        @Header("Authorization") token: String,
        @Body request: WithdrawalRequest
    ): Call<WithdrawalResponse>

    @GET("driver/get-bank-details/{driverId}")
    fun getBankDetails(
        @Header("Authorization") token: String,
        @Path("driverId") driverId: Long
    ): Call<DriverBankDetailsResponse>

    @POST("driver/update-bank-details")
    fun updateBankDetails(
        @Header("Authorization") token: String,
        @Body request: UpdateBankDetailsRequest
    ): Call<UpdateBankDetailsResponse>

    @GET("driver/all-withdrawal-request/{driverId}")
    fun getAllWithdrawRequests(
        @Header("Authorization") token: String,
        @Path("driverId") driverId: Long
    ): Call<List<WithdrawRequest>>

    @POST("auth/login")
    fun login(
        @Body body: LoginBody
    ): Call<ResponseBody>
}
