package com.indiacybercafe.printhub.models

data class TicketModel(
    val ticketId: String = "",
    val uid: String = "",
    val userName: String = "",
    val phone: String = "",
    val orderId: String = "",
    val category: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val status: String = "Open",
    val createdAt: Long = System.currentTimeMillis()
)
