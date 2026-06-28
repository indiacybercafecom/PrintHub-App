package com.indiacybercafe.printhub.models

data class FaqModel(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)
