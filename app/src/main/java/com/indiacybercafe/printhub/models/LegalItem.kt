package com.indiacybercafe.printhub.models

data class LegalItem(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val url: String? = null,
    val isClickable: Boolean = true
)
