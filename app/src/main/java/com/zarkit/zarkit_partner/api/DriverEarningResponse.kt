package com.zarkit.zarkit_partner.api

import com.google.gson.annotations.SerializedName

data class DriverEarningResponse(
    val earning: Double,
    val firstName: String,
    val lastName: String,
    val driverId: Int,
    @SerializedName("isblocked")
    val isBlocked: Boolean
)
