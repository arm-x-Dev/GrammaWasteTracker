package com.example.grammawastetracker.data.repository

import com.example.grammawastetracker.data.model.DriverLocation
import com.example.grammawastetracker.data.remote.FirebaseService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationRepository(private val firebaseService: FirebaseService) {

    fun updateDriverLocation(lat: Double, lng: Double) {
        val driverUid = firebaseService.auth.currentUser?.uid ?: return
        val location = DriverLocation(driverUid, lat, lng, onDuty = true)
        firebaseService.database.child("driverLocations").child(driverUid).setValue(location)
        firebaseService.database.child("driverLocations").child(driverUid).onDisconnect().removeValue()
    }
    
    fun removeDriverLocation() {
        val driverUid = firebaseService.auth.currentUser?.uid ?: return
        firebaseService.database.child("driverLocations").child(driverUid).removeValue()
    }

    fun getDriverLocation(driverUid: String): Flow<DriverLocation?> = callbackFlow {
        if (driverUid.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        
        val listener = firebaseService.database.child("driverLocations").child(driverUid)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val loc = snapshot.getValue(DriverLocation::class.java)
                    trySend(loc)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            })
            
        awaitClose { firebaseService.database.child("driverLocations").child(driverUid).removeEventListener(listener) }
    }
    
    fun getAllDriverLocations(): Flow<List<DriverLocation>> = callbackFlow {
        val listener = firebaseService.database.child("driverLocations")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val locs = snapshot.children.mapNotNull { it.getValue(DriverLocation::class.java) }
                        .filter { it.onDuty }
                    trySend(locs)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            })
            
        awaitClose { firebaseService.database.child("driverLocations").removeEventListener(listener) }
    }
}
