package com.zarkit.zarkit_partner.api

data class RazorpayOrderResponse(
    val orderId: String,
    val amount: Int,
    val currency: String
)