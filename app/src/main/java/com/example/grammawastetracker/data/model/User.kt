package com.example.grammawastetracker.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val role: String = "", // RESIDENT | DRIVER
    val fcmToken: String = ""
)
