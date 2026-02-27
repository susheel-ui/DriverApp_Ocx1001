package com.zarkit.zarkit_partner.api

data class QrResponse(
    val transactionId: Long,
    val qrUrl: String,
    val amount: Long
)
