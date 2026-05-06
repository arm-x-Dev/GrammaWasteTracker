package com.example.grammawastetracker.ui.resident

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grammawastetracker.data.model.DriverLocation
import com.example.grammawastetracker.data.model.GarbageReport
import com.example.grammawastetracker.data.remote.FirebaseService
import com.example.grammawastetracker.data.repository.GarbageRepository
import com.example.grammawastetracker.data.repository.LocationRepository
import kotlinx.coroutines.launch

class ResidentDashboardViewModel : ViewModel() {
    private val firebaseService = FirebaseService()
    private val garbageRepository = GarbageRepository(firebaseService)
    private val locationRepository = LocationRepository(firebaseService)

    private val _uploadResult = MutableLiveData<Boolean>()
    val uploadResult: LiveData<Boolean> = _uploadResult

    private val _activeReport = MutableLiveData<GarbageReport?>()
    val activeReport: LiveData<GarbageReport?> = _activeReport

    private val _driverLocation = MutableLiveData<DriverLocation?>()
    val driverLocation: LiveData<DriverLocation?> = _driverLocation

    private val _residentHistory = MutableLiveData<List<GarbageReport>>()
    val residentHistory: LiveData<List<GarbageReport>> = _residentHistory

    private val _allDrivers = MutableLiveData<List<DriverLocation>>()
    val allDrivers: LiveData<List<DriverLocation>> = _allDrivers

    init {
        observeActiveReport()
        observeResidentHistory()
        observeAllDrivers()
    }

    private fun observeAllDrivers() {
        viewModelScope.launch {
            locationRepository.getAllDriverLocations().collect { drivers ->
                _allDrivers.value = drivers
            }
        }
    }

    private fun observeResidentHistory() {
        viewModelScope.launch {
            garbageRepository.getResidentHistory().collect { reports ->
                _residentHistory.value = reports
            }
        }
    }

    fun submitReport(imageUri: Uri, lat: Double, lng: Double) {
        viewModelScope.launch {
            _uploadResult.value = garbageRepository.submitReport(imageUri, lat, lng)
        }
    }

    fun resetUploadResult() {
        _uploadResult.value = false
    }

    private fun observeActiveReport() {
        viewModelScope.launch {
            garbageRepository.getActiveReportForResident().collect { report ->
                _activeReport.value = report
                if (report != null && report.status == "PENDING" && report.driverUid.isNotEmpty()) {
                    observeDriverLocation(report.driverUid)
                } else if (report != null && report.status == "PENDING") {
                    // Wait for a driver to accept. If they haven't, we can try to show all drivers or closest driver, 
                    // but according to prompt, track "driver's live location pin". 
                    // Let's observe all and pick nearest, or just observe the one assigned.
                    // Assuming report.driverUid gets populated when a driver accepts it.
                } else {
                    _driverLocation.value = null
                }
            }
        }
    }

    private fun observeDriverLocation(driverUid: String) {
        viewModelScope.launch {
            locationRepository.getDriverLocation(driverUid).collect { loc ->
                _driverLocation.value = loc
            }
        }
    }
}
