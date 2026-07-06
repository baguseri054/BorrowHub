package com.borrowhub.app.data

import com.google.firebase.Timestamp

data class Items(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val condition: String = "",
    val imageUrl: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    @field:JvmField val isAvailable: Boolean = true
)