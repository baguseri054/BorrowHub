package com.borrowhub.app.data

import com.google.firebase.Timestamp

data class Borrow(
    var id: String = "",
    var itemId: String = "",
    var itemName: String = "",
    var borrowerName: String = "",
    var startDate: Timestamp? = null,
    var endDate: Timestamp? = null,
    var photoBefore: String = "",
    var status: String = "Active", // Active, Completed, Overdue
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)