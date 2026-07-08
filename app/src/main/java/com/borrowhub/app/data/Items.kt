package com.borrowhub.app.data

import com.google.firebase.Timestamp

data class Items(
    var id: String = "", // Menyimpan Document ID dari Firestore
    var name: String = "",
    var description: String = "",
    var condition: String = "",
    var imageUrl: String = "",
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
    var isAvailable: Boolean = true
)