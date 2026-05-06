package com.example.grammawastetracker.data.repository

import android.net.Uri
import com.example.grammawastetracker.data.model.GarbageReport
import com.example.grammawastetracker.data.remote.FirebaseService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GarbageRepository(private val firebaseService: FirebaseService) {

    suspend fun submitReport(imageUri: Uri, lat: Double, lng: Double): Boolean {
        return try {
            val reportId = UUID.randomUUID().toString()
            val imageUrl = firebaseService.uploadGarbageImage(reportId, imageUri)
            val residentUid = firebaseService.auth.currentUser?.uid ?: return false
            
            val report = GarbageReport(
                id = reportId,
                imageUrl = imageUrl,
                lat = lat,
                lng = lng,
                status = "PENDING",
                residentUid = residentUid,
                driverUid = ""
            )
            
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                firebaseService.database.child("garbageReports").child(reportId).setValue(report).await()
                true
            } ?: false
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun acceptReport(reportId: String, driverUid: String): Boolean {
        return try {
            val updates = mapOf(
                "driverUid" to driverUid
            )
            firebaseService.database.child("garbageReports").child(reportId).updateChildren(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAsCollected(reportId: String, driverUid: String, proofImageUri: Uri?): Boolean {
        return try {
            val proofUrl = if (proofImageUri != null) {
                firebaseService.uploadGarbageImage("proof_$reportId", proofImageUri)
            } else ""

            val updates = mapOf(
                "status" to "COLLECTED",
                "driverUid" to driverUid,
                "proofImageUrl" to proofUrl
            )
            firebaseService.database.child("garbageReports").child(reportId).updateChildren(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getPendingReports(): Flow<List<GarbageReport>> = callbackFlow {
        val listener = firebaseService.database.child("garbageReports")
            .orderByChild("status")
            .equalTo("PENDING")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val reports = snapshot.children.mapNotNull { it.getValue(GarbageReport::class.java) }
                    trySend(reports)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            })
            
        awaitClose { firebaseService.database.child("garbageReports").removeEventListener(listener) }
    }
    
    fun getActiveReportForResident(): Flow<GarbageReport?> = callbackFlow {
        val uid = firebaseService.auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            return@callbackFlow
        }
        
        val listener = firebaseService.database.child("garbageReports")
            .orderByChild("residentUid")
            .equalTo(uid)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val reports = snapshot.children.mapNotNull { it.getValue(GarbageReport::class.java) }
                    // Find latest pending report
                    val pendingReport = reports.filter { it.status == "PENDING" }.maxByOrNull { it.timestamp }
                    trySend(pendingReport)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            })
            
        awaitClose { firebaseService.database.child("garbageReports").removeEventListener(listener) }
    }

    fun getResidentHistory(): Flow<List<GarbageReport>> = callbackFlow {
        val uid = firebaseService.auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            return@callbackFlow
        }
        
        val listener = firebaseService.database.child("garbageReports")
            .orderByChild("residentUid")
            .equalTo(uid)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val reports = snapshot.children.mapNotNull { it.getValue(GarbageReport::class.java) }
                    // Sort descending by timestamp so newest is on top
                    trySend(reports.sortedByDescending { it.timestamp })
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            })
            
        awaitClose { firebaseService.database.child("garbageReports").removeEventListener(listener) }
    }

    fun getDriverCompletedReports(): Flow<List<GarbageReport>> = callbackFlow {
        val uid = firebaseService.auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            return@callbackFlow
        }
        
        val listener = firebaseService.database.child("garbageReports")
            .orderByChild("driverUid")
            .equalTo(uid)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val reports = snapshot.children.mapNotNull { it.getValue(GarbageReport::class.java) }
                    // Filter for COLLECTED only
                    trySend(reports.filter { it.status == "COLLECTED" }.sortedByDescending { it.timestamp })
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            })
            
        awaitClose { firebaseService.database.child("garbageReports").removeEventListener(listener) }
    }
}
