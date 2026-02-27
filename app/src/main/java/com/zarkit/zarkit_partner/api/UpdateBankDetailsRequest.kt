package com.zarkit.zarkit_partner.api

data class UpdateBankDetailsRequest(
    val driverId: Long,
    val accountNumber: String,
    val ifscCode: String
)
