package com.zarkit.zarkit_partner.api

data class WithdrawalRequest(
    val userId: Long,
    val amount: Double
)