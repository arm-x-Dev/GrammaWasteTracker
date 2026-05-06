package com.example.grammawastetracker.ui.common

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.grammawastetracker.R
import com.example.grammawastetracker.data.remote.FirebaseService
import com.example.grammawastetracker.data.repository.AuthRepository
import com.example.grammawastetracker.ui.auth.LoginActivity
import com.example.grammawastetracker.ui.driver.DriverDashboardActivity
import com.example.grammawastetracker.ui.resident.ResidentDashboardActivity
import com.example.grammawastetracker.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        val firebaseService = FirebaseService()
        authRepository = AuthRepository(firebaseService)

        lifecycleScope.launch {
            // Wait for motion layout animation
            delay(2500)
            requestPermissionsAndProceed()
        }
    }

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    private fun requestPermissionsAndProceed() {
        val missingPermissions = requiredPermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray(), 1001)
        } else {
            lifecycleScope.launch { checkAuthAndRoute() }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            // Proceed regardless of whether all were granted, but we've asked early as requested
            lifecycleScope.launch { checkAuthAndRoute() }
        }
    }

    private suspend fun checkAuthAndRoute() {
        if (authRepository.isUserLoggedIn()) {
            val role = authRepository.getUserRole()
            when (role) {
                Constants.ROLE_RESIDENT -> {
                    startActivity(Intent(this, ResidentDashboardActivity::class.java))
                }
                Constants.ROLE_DRIVER -> {
                    startActivity(Intent(this, DriverDashboardActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
        } else {
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        }
        finish()
    }
}
