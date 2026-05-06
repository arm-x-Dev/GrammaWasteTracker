package com.example.grammawastetracker.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import kotlinx.coroutines.tasks.await

class FirebaseService {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://gramawastetracker-271ff-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    suspend fun uploadGarbageImage(reportId: String, imageUri: Uri): String {
        // Since Firebase Storage requires a paid plan, we will skip the upload
        // and just return the local file path as a string.
        // The driver's device will show a placeholder image instead.
        return imageUri.toString()
    }
}
