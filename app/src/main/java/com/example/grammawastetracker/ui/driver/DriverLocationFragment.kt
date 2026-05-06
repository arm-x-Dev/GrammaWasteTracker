package com.example.grammawastetracker.ui.driver

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.FragmentDriverLocationBinding
import com.example.grammawastetracker.service.DriverLocationService
import com.example.grammawastetracker.utils.PermissionHelper
import com.example.grammawastetracker.utils.setup

class DriverLocationFragment : Fragment() {

    private var _binding: FragmentDriverLocationBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")
    
    private fun getBindingSafe(): FragmentDriverLocationBinding? = _binding
    
    private val viewModel: DriverDashboardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDriverLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val driverMarkers = mutableMapOf<String, org.osmdroid.views.overlay.Marker>()
    private val trashMarkers = mutableMapOf<String, org.osmdroid.views.overlay.Marker>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.mapView.setup(requireContext())
        binding.mapView.controller.setZoom(15.0)
        
        // Auto-center on current location
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())
        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                getBindingSafe()?.let { safeBinding ->
                    if (location != null) {
                        val point = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                        safeBinding.mapView.controller.animateTo(point, 17.5, 1200L)
                    } else {
                        safeBinding.mapView.controller.setCenter(org.osmdroid.util.GeoPoint(12.9716, 77.5946)) // Fallback to Bengaluru
                    }
                }
            }
        } catch (e: SecurityException) {
            getBindingSafe()?.mapView?.controller?.setCenter(org.osmdroid.util.GeoPoint(12.9716, 77.5946)) // Fallback
        }
        
        // Clear overlays on start to ensure a clean state
        binding.mapView.overlays.clear()
        driverMarkers.clear()
        trashMarkers.clear()
        
        viewModel.isDutyActive.observe(viewLifecycleOwner) { isActive ->
            binding.dutyToggle.isChecked = isActive
            binding.dutyText.text = if (isActive) getString(R.string.status_on_duty) else getString(R.string.status_off_duty)
        }

        binding.dutyToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (PermissionHelper.hasLocationPermissions(requireContext())) {
                    startDuty()
                } else {
                    binding.dutyToggle.isChecked = false
                    PermissionHelper.requestLocationPermissions(requireActivity(), 100)
                }
            } else {
                stopDuty()
            }
        }

        binding.myLocationButton.setOnClickListener {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())
            try {
                fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    getBindingSafe()?.let { safeBinding ->
                        if (location != null) {
                            val point = org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                            safeBinding.mapView.controller.animateTo(point, 18.0, 1000L)
                        } else {
                            android.widget.Toast.makeText(requireContext(), "Searching for location...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Handle permissions
            }
        }

        // Observe other drivers
        viewModel.allDrivers.observe(viewLifecycleOwner) { drivers ->
            drivers.forEach { driver ->
                val point = org.osmdroid.util.GeoPoint(driver.lat, driver.lng)
                if (driverMarkers.containsKey(driver.driverUid)) {
                    driverMarkers[driver.driverUid]?.position = point
                } else {
                    val marker = org.osmdroid.views.overlay.Marker(binding.mapView).apply {
                        position = point
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_truck_marker)
                        setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                        title = "Driver"
                    }
                    binding.mapView.overlays.add(marker)
                    driverMarkers[driver.driverUid] = marker
                }
            }
            binding.mapView.invalidate()
        }

        // Observe trash locations
        viewModel.pendingReports.observe(viewLifecycleOwner) { reports ->
            // Remove markers for reports that are no longer pending
            val reportIds = reports.map { it.id }.toSet()
            val iterator = trashMarkers.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!reportIds.contains(entry.key)) {
                    binding.mapView.overlays.remove(entry.value)
                    iterator.remove()
                }
            }

            // Add or update markers for pending reports
            reports.forEach { report ->
                val point = org.osmdroid.util.GeoPoint(report.lat, report.lng)
                if (!trashMarkers.containsKey(report.id)) {
                    val marker = org.osmdroid.views.overlay.Marker(binding.mapView).apply {
                        position = point
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash_marker)
                        setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                        title = "Trash to collect"
                    }
                    binding.mapView.overlays.add(marker)
                    trashMarkers[report.id] = marker
                }
            }
            binding.mapView.invalidate()
        }
    }

    private fun startDuty() {
        viewModel.setDutyStatus(true)
        val intent = Intent(requireContext(), DriverLocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun stopDuty() {
        viewModel.setDutyStatus(false)
        val intent = Intent(requireContext(), DriverLocationService::class.java)
        requireContext().stopService(intent)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDetach()
        _binding = null
    }
}
