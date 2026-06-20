package com.indiacybercafe.printhub

data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "general", // "success", "payment", "welcome", "general"
    val timestamp: Long = 0L,
    var isRead: Boolean = false,
    val orderId: String = "",
    var isExpanded: Boolean = false
)
