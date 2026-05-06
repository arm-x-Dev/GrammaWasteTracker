package com.example.grammawastetracker.data.repository

import com.example.grammawastetracker.data.model.User
import com.example.grammawastetracker.data.remote.FirebaseService
import kotlinx.coroutines.tasks.await

class AuthRepository(private val firebaseService: FirebaseService) {

    suspend fun login(email: String, password: String): Boolean {
        return try {
            firebaseService.auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun register(email: String, password: String, name: String, role: String): String? {
        return try {
            val result = firebaseService.auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return "Failed to get User ID"
            
            // Also set displayName in Auth for convenience
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            try {
                val user = User(uid = uid, name = name, role = role)
                kotlinx.coroutines.withTimeout(10000) {
                    firebaseService.database.child("users").child(uid).setValue(user).await()
                }
                firebaseService.auth.signOut()
                null // success
            } catch (e: Exception) {
                // If database write fails, delete the auth user so they can try again with same email
                result.user?.delete()?.await()
                e.printStackTrace()
                "Database error: ${e.message}. Please try again."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.message ?: "Unknown error occurred"
        }
    }

    suspend fun getUserDetails(): User? {
        val uid = firebaseService.auth.currentUser?.uid ?: return null
        return try {
            kotlinx.coroutines.withTimeoutOrNull(5000) {
                val snapshot = firebaseService.database.child("users").child(uid).get().await()
                snapshot.getValue(User::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserRole(): String? {
        val uid = firebaseService.auth.currentUser?.uid ?: return null
        return try {
            kotlinx.coroutines.withTimeoutOrNull(5000) {
                val snapshot = firebaseService.database.child("users").child(uid).get().await()
                val user = snapshot.getValue(User::class.java)
                user?.role
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun updateFcmToken(token: String) {
        val uid = firebaseService.auth.currentUser?.uid ?: return
        try {
            firebaseService.database.child("users").child(uid).child("fcmToken").setValue(token).await()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseService.auth.currentUser != null
    }

    fun logout() {
        firebaseService.auth.signOut()
    }
}
