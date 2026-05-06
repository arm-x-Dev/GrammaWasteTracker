package com.example.grammawastetracker.data.model

data class DriverLocation(
    val driverUid: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val onDuty: Boolean = false
)
