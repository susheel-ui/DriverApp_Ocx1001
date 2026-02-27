package com.zarkit.zarkit_partner.api

data class RideDetailsResponse(
    val rideId: Long,
    val pickupLat: Double,
    val pickupLon: Double,
    val pickupAddress: String,
    val dropLat: Double,
    val dropLon: Double,
    val dropAddress: String,
    val distanceText: String,
    val durationText: String,
    val finalFare: Double,
    val status: String
)
