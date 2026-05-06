package com.example.grammawastetracker.ui.driver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grammawastetracker.data.model.GarbageReport
import com.example.grammawastetracker.data.remote.FirebaseService
import com.example.grammawastetracker.data.repository.GarbageRepository
import kotlinx.coroutines.launch

class DriverDashboardViewModel : ViewModel() {
    private val firebaseService = FirebaseService()
    private val garbageRepository = GarbageRepository(firebaseService)
    private val locationRepository = com.example.grammawastetracker.data.repository.LocationRepository(firebaseService)

    private val _pendingReports = MutableLiveData<List<GarbageReport>>()
    val pendingReports: LiveData<List<GarbageReport>> = _pendingReports

    private val _completedReports = MutableLiveData<List<GarbageReport>>()
    val completedReports: LiveData<List<GarbageReport>> = _completedReports

    private val _allDrivers = MutableLiveData<List<com.example.grammawastetracker.data.model.DriverLocation>>()
    val allDrivers: LiveData<List<com.example.grammawastetracker.data.model.DriverLocation>> = _allDrivers

    private val _isDutyActive = MutableLiveData<Boolean>(com.example.grammawastetracker.service.DriverLocationService.isRunning)
    val isDutyActive: LiveData<Boolean> = _isDutyActive
    
    private val _collectionResult = MutableLiveData<Boolean>()
    val collectionResult: LiveData<Boolean> = _collectionResult

    init {
        _isDutyActive.value = com.example.grammawastetracker.service.DriverLocationService.isRunning
        observePendingReports()
        observeCompletedReports()
        observeAllDrivers()
    }

    private fun observeCompletedReports() {
        viewModelScope.launch {
            garbageRepository.getDriverCompletedReports().collect { reports ->
                _completedReports.value = reports
            }
        }
    }

    private fun observeAllDrivers() {
        viewModelScope.launch {
            locationRepository.getAllDriverLocations().collect { drivers ->
                _allDrivers.value = drivers
            }
        }
    }

    private fun observePendingReports() {
        viewModelScope.launch {
            garbageRepository.getPendingReports().collect { reports ->
                _pendingReports.value = reports
            }
        }
    }

    fun setDutyStatus(active: Boolean) {
        _isDutyActive.value = active
    }

    fun acceptReport(reportId: String) {
        viewModelScope.launch {
            val driverUid = firebaseService.auth.currentUser?.uid ?: return@launch
            garbageRepository.acceptReport(reportId, driverUid)
        }
    }

    fun markAsCollected(reportId: String, proofImageUri: android.net.Uri? = null) {
        viewModelScope.launch {
            val driverUid = firebaseService.auth.currentUser?.uid ?: return@launch
            _collectionResult.value = garbageRepository.markAsCollected(reportId, driverUid, proofImageUri)
        }
    }

    fun resetCollectionResult() {
        _collectionResult.value = false
    }
}
