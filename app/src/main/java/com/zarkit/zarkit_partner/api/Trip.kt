package com.zarkit.zarkit_partner.api

data class Trip(
    val id: Long,
    val pickupAddress: String?,
    val dropAddress: String?,
    val finalFare: Double?,
    val distanceText: String?,
    val paymentStatus: String?,
    val status: String?,
    val createdAt: String?,
    val driverRideEarning: Double? = 0.0
)
