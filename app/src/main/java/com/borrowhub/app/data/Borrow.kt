package com.borrowhub.app.data

import com.google.firebase.Timestamp

data class Borrow(
    var id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val borrowerName: String = "",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val photoBefore: String = "",
    val status: String = "Active", // Active, Completed, Overdue
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)