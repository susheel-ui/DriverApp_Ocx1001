package com.zarkit.zarkit_partner.api

data class WithdrawalResponse(
    val status: String,   // "OK" or "FAILED"
    val message: String
)