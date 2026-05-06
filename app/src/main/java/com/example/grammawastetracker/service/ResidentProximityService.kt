package com.example.grammawastetracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.grammawastetracker.R
import com.example.grammawastetracker.data.model.DriverLocation
import com.example.grammawastetracker.data.remote.FirebaseService
import com.example.grammawastetracker.data.repository.LocationRepository
import com.example.grammawastetracker.ui.common.SplashActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class ResidentProximityService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var locationRepository: LocationRepository
    
    private var homeLat: Double = 0.0
    private var homeLng: Double = 0.0
    private var hasNotified = false

    companion object {
        private const val MONITORING_CHANNEL_ID = "resident_monitoring_channel"
        private const val ARRIVAL_CHANNEL_ID = "resident_arrival_channel"
        private const val NOTIFICATION_ID = 505
        
        fun start(context: Context, lat: Double, lng: Double) {
            val intent = Intent(context, ResidentProximityService::class.java).apply {
                putExtra("lat", lat)
                putExtra("lng", lng)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, ResidentProximityService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationRepository = LocationRepository(FirebaseService())
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        homeLat = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
        homeLng = intent?.getDoubleExtra("lng", 0.0) ?: 0.0
        
        startForeground(NOTIFICATION_ID, createStatusNotification())
        
        observeDrivers()
        
        return START_STICKY
    }

    private fun observeDrivers() {
        serviceScope.launch {
            locationRepository.getAllDriverLocations().collect { drivers ->
                checkProximity(drivers)
            }
        }
    }

    private fun checkProximity(drivers: List<DriverLocation>) {
        if (homeLat == 0.0 || homeLng == 0.0) return
        
        var anyNearby = false
        drivers.forEach { driver ->
            val results = FloatArray(1)
            Location.distanceBetween(homeLat, homeLng, driver.lat, driver.lng, results)
            val distance = results[0].toInt()
            
            if (distance <= 300) {
                anyNearby = true
                if (!hasNotified) {
                    sendArrivalNotification()
                    hasNotified = true
                }
            }
        }
        
        if (!anyNearby) {
            if (hasNotified) {
                hasNotified = false
            }
        }
    }

    private fun sendArrivalNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ARRIVAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createStatusNotification(): Notification {
        val intent = Intent(this, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
            .setContentTitle("Grama-Waste Active")
            .setContentText("Monitoring nearby trucks...")
            .setSmallIcon(R.drawable.ic_trash_marker)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            // Delete old combined channel if it exists from previous version
            manager.deleteNotificationChannel("resident_proximity_channel")
            
            // 1. Silent Monitoring Channel
            val monitoringChannel = NotificationChannel(
                MONITORING_CHANNEL_ID,
                "Background Monitoring",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            
            // 2. Loud Arrival Channel
            val arrivalChannel = NotificationChannel(
                ARRIVAL_CHANNEL_ID,
                "Truck Arrival Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            manager.createNotificationChannel(monitoringChannel)
            manager.createNotificationChannel(arrivalChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
