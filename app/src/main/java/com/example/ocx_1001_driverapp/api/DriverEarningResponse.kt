package com.example.ocx_1001_driverapp.api

data class DriverEarningResponse(
    val earning: Double,
    val commission: Double,
    val firstName: String,
    val lastName: String,
    val driverId: Int
)
