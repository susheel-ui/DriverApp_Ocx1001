package com.zarkit.zarkit_partner.api

data class RazorpayVerifyRequest(
    val paymentId: String,
    val orderId: String,
    val signature: String
)
