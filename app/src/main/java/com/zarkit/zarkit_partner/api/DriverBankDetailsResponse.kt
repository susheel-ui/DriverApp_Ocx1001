package com.zarkit.zarkit_partner.api

data class DriverBankDetailsResponse(
    val driverId: Long,
    val accountNumber: String?,   // null if no data
    val ifscCode: String?         // null if no data
)
