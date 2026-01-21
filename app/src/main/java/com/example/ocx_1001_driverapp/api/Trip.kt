package com.example.ocx_1001_driverapp.api

data class Trip(
    val finalFare: Double,
    val pickupAddress: String,
    val dropAddress: String,
    val distanceText: String
)
