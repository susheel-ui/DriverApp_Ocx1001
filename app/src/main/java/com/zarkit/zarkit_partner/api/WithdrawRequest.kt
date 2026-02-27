package com.zarkit.zarkit_partner.api

data class WithdrawRequest(
    val id: Long,
    val userId: Long,
    val userName: String,
    val userPhone: String,
    val amount: Double,
    val accountNumber: String?,
    val ifscCode: String?,
    val status: String,
    val requestTime: String
)
