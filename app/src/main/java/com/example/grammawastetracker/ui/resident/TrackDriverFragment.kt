package com.example.grammawastetracker.ui.resident

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.FragmentTrackDriverBinding
import com.example.grammawastetracker.utils.setup
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class TrackDriverFragment : Fragment() {

    private var _binding: FragmentTrackDriverBinding? = null
    // Safer binding access for async callbacks
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")
    
    private fun getBindingSafe(): FragmentTrackDriverBinding? = _binding
    
    private val viewModel: ResidentDashboardViewModel by activityViewModels()

    private var driverMarker: Marker? = null
    private var residentMarker: Marker? = null
    private var polyline: Polyline? = null
    private var hasNotifiedNearby = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrackDriverBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val otherDriverMarkers = mutableMapOf<String, Marker>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.mapView.setup(requireContext())
        binding.mapView.controller.setZoom(15.0)
        
        // Auto-center on current location
        val context = context ?: return
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                getBindingSafe()?.let { safeBinding ->
                    if (location != null) {
                        val point = GeoPoint(location.latitude, location.longitude)
                        safeBinding.mapView.controller.setCenter(point)
                        safeBinding.mapView.controller.setZoom(17.5)
                        
                        // Create initial resident marker for proximity tracking
                        if (residentMarker == null) {
                            residentMarker = Marker(safeBinding.mapView).apply {
                                icon = ContextCompat.getDrawable(context, R.drawable.ic_home_marker)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = getString(R.string.your_location)
                            }
                            safeBinding.mapView.overlays.add(residentMarker)
                        }
                        residentMarker?.position = point
                    } else {
                        safeBinding.mapView.controller.setCenter(GeoPoint(12.9716, 77.5946)) // Fallback to Bengaluru
                    }
                }
            }
        } catch (e: SecurityException) {
            getBindingSafe()?.mapView?.controller?.setCenter(GeoPoint(12.9716, 77.5946)) // Fallback
        }
        
        // Initial state
        binding.mapView.overlays.clear()
        otherDriverMarkers.clear()
        residentMarker = null
        driverMarker = null

        viewModel.activeReport.observe(viewLifecycleOwner) { report ->
            if (report == null || report.status != "PENDING") {
                binding.etaText.text = getString(R.string.no_active_pickup)
                binding.etaText.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surface))
                binding.etaText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                // Remove resident/trash marker if it exists
                residentMarker?.let { binding.mapView.overlays.remove(it) }
                residentMarker = null
            } else {
                val trashLoc = GeoPoint(report.lat, report.lng)
                // We no longer show the trash marker itself, just focus the map or show ETA
                // binding.mapView.controller.animateTo(trashLoc) // Only focus if user asks or first load
            }
        }

        viewModel.allDrivers.observe(viewLifecycleOwner) { drivers ->
            val context = context ?: return@observe
            val homePoint = residentMarker?.position ?: viewModel.activeReport.value?.let { GeoPoint(it.lat, it.lng) }

            // 1. CLEANUP: Remove markers for drivers who are no longer in the list (Off Duty)
            val activeDriverIds = drivers.map { it.driverUid }.toSet()
            val iterator = otherDriverMarkers.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!activeDriverIds.contains(entry.key)) {
                    binding.mapView.overlays.remove(entry.value)
                    iterator.remove()
                }
            }

            var minDistance = Int.MAX_VALUE
            var anyNearby = false

            // 2. UPDATE: Add or move markers for all current on-duty drivers
            drivers.forEach { driver ->
                val point = GeoPoint(driver.lat, driver.lng)
                
                val isActiveDriver = viewModel.activeReport.value?.driverUid == driver.driverUid

                if (!isActiveDriver) {
                    if (otherDriverMarkers.containsKey(driver.driverUid)) {
                        val marker = otherDriverMarkers[driver.driverUid]
                        val oldPos = marker?.position
                        if (oldPos != null && (oldPos.latitude != point.latitude || oldPos.longitude != point.longitude)) {
                            animateMarker(marker!!, oldPos, point)
                        } else {
                            marker?.position = point
                        }
                    } else {
                        val marker = Marker(binding.mapView).apply {
                            position = point
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_truck_marker)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = getString(R.string.waste_truck)
                        }
                        binding.mapView.overlays.add(marker)
                        otherDriverMarkers[driver.driverUid] = marker
                    }
                } else {
                    otherDriverMarkers[driver.driverUid]?.let { 
                        binding.mapView.overlays.remove(it)
                        otherDriverMarkers.remove(driver.driverUid)
                    }
                }

                // Calculate distance for this truck
                if (homePoint != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(homePoint.latitude, homePoint.longitude, driver.lat, driver.lng, results)
                    val distance = results[0].toInt()
                    
                    if (distance < minDistance) minDistance = distance
                    if (distance <= 300) anyNearby = true
                }
            }

            // 3. UI UPDATE: Update status pill once based on best information
            if (anyNearby) {
                binding.etaText.text = getString(R.string.vehicle_nearby)
                binding.etaText.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.forest_green))
                binding.etaText.setTextColor(ContextCompat.getColor(context, R.color.on_primary))
                
                if (!hasNotifiedNearby) {
                    sendLocalNotification()
                    hasNotifiedNearby = true
                }
            } else {
                val isActiveReport = viewModel.activeReport.value != null
                if (isActiveReport && minDistance != Int.MAX_VALUE) {
                    binding.etaText.text = getString(R.string.driver_away, minDistance)
                    binding.etaText.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface))
                    binding.etaText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                } else {
                    binding.etaText.text = if (isActiveReport) getString(R.string.searching_for_driver) else getString(R.string.no_active_pickup)
                    binding.etaText.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface))
                    binding.etaText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
                
                // Allow re-notifying if truck moves far away and comes back
                if (minDistance > 500) hasNotifiedNearby = false
            }

            binding.mapView.invalidate()
        }

        viewModel.driverLocation.observe(viewLifecycleOwner) { loc ->
            if (loc != null) {
                val driverPoint = GeoPoint(loc.lat, loc.lng)
                
                if (driverMarker == null) {
                    val context = context ?: return@observe
                    driverMarker = Marker(binding.mapView).apply {
                        position = driverPoint
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_truck_marker)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = getString(R.string.your_assigned_driver)
                    }
                    binding.mapView.overlays.add(driverMarker)
                } else {
                    val oldPoint = driverMarker?.position
                    if (oldPoint != null && (oldPoint.latitude != driverPoint.latitude || oldPoint.longitude != driverPoint.longitude)) {
                        animateMarker(driverMarker!!, oldPoint, driverPoint)
                    } else {
                        driverMarker?.position = driverPoint
                    }
                }
                binding.mapView.invalidate()
            } else {
                driverMarker?.let { binding.mapView.overlays.remove(it) }
                driverMarker = null
            }
        }

        binding.myLocationButton.setOnClickListener {
            val context = context ?: return@setOnClickListener
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            try {
                // Use modern getCurrentLocation for better reliability than lastLocation
                fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val context = context ?: return@addOnSuccessListener
                        getBindingSafe()?.let { safeBinding ->
                            val point = GeoPoint(location.latitude, location.longitude)
                            
                            // Update resident pin
                            if (residentMarker == null) {
                                residentMarker = Marker(safeBinding.mapView).apply {
                                    icon = ContextCompat.getDrawable(context, R.drawable.ic_home_marker)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = getString(R.string.your_location)
                                }
                                safeBinding.mapView.overlays.add(residentMarker)
                            }
                            residentMarker?.position = point
                            
                            safeBinding.mapView.controller.animateTo(point, 18.0, 1000L)
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Waiting for GPS signal...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) { }
        }
    }

    private fun animateMarker(marker: Marker, start: GeoPoint, end: GeoPoint) {
        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1500 // 1.5 seconds for smooth glide
        animator.interpolator = android.view.animation.LinearInterpolator()
        
        val startLat = start.latitude
        val startLng = start.longitude
        val endLat = end.latitude
        val endLng = end.longitude

        // Calculate rotation once at the start of movement
        val bearing = calculateBearing(start, end)
        if (bearing != 0f) {
            marker.rotation = bearing
        }

        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val lat = startLat + (endLat - startLat) * fraction
            val lng = startLng + (endLng - startLng) * fraction
            marker.position = GeoPoint(lat, lng)
            _binding?.mapView?.invalidate()
        }
        animator.start()
    }

    private fun calculateBearing(start: GeoPoint, end: GeoPoint): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val y = Math.sin(lon2 - lon1) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)
        val bearing = Math.toDegrees(Math.atan2(y, x)).toFloat()
        return (bearing + 360) % 360
    }

    private fun sendLocalNotification() {
        val context = context ?: return
        
        // DEBUG TOAST: If you see this, the math worked!
        android.widget.Toast.makeText(context, "Vehicle Proximity Triggered!", android.widget.Toast.LENGTH_LONG).show()
        
        val channelId = "proximity_alerts"
        val notificationId = 101

        val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Proximity Alerts", android.app.NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Alerts when waste vehicle is nearby"
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system icon for reliability
            .setContentTitle(getString(R.string.vehicle_nearby))
            .setContentText(getString(R.string.notification_body))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
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
