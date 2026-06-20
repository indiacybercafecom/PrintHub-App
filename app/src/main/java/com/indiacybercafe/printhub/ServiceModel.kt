package com.indiacybercafe.printhub

data class ServiceModel(
    var id: String = "",
    var title: String = "",
    var iconUrl: String = "",
    var enabled: Boolean = true,
    var order: Int = 0,
    var action: String = "",
    var badge: String = "",
    var premium: Boolean = false,
    var backgroundColor: String = "#FFFFFF"
) {
    // Constructor for local service repository
    constructor(title: String, action: String, iconRes: Int, backgroundColor: String, enabled: Boolean) : this(
        id = "",
        title = title,
        iconUrl = "",
        enabled = enabled,
        order = 0,
        action = action,
        badge = "",
        premium = false,
        backgroundColor = backgroundColor
    )
}
