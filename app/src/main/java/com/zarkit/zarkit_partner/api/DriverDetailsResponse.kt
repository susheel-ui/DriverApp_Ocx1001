package com.zarkit.zarkit_partner.api

data class DriverDetailsResponse(
    val firstName: String,
    val lastName: String,
    val email: String?,
    val mobile: String,
    val earning: Double
)
