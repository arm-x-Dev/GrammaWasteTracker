package com.example.grammawastetracker.data.model

data class GarbageReport(
    val id: String = "",
    val imageUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "PENDING", // PENDING | COLLECTED
    val residentUid: String = "",
    val driverUid: String = "",
    val proofImageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
