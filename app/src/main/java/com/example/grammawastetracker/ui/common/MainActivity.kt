package com.example.grammawastetracker.ui.common

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.grammawastetracker.data.remote.FirebaseService
import com.example.grammawastetracker.data.repository.AuthRepository
import com.example.grammawastetracker.ui.auth.LoginActivity
import com.example.grammawastetracker.ui.driver.DriverDashboardActivity
import com.example.grammawastetracker.ui.resident.ResidentDashboardActivity
import com.example.grammawastetracker.R
import androidx.lifecycle.lifecycleScope
import com.example.grammawastetracker.utils.Constants
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val authRepository = AuthRepository(FirebaseService())
        
        lifecycleScope.launch {
            if (authRepository.isUserLoggedIn()) {
                val role = authRepository.getUserRole()
                when (role) {
                    Constants.ROLE_RESIDENT -> {
                        startActivity(Intent(this@MainActivity, ResidentDashboardActivity::class.java))
                        finish()
                    }
                    Constants.ROLE_DRIVER -> {
                        startActivity(Intent(this@MainActivity, DriverDashboardActivity::class.java))
                        finish()
                    }
                    else -> {
                        // Role missing or timed out
                        android.widget.Toast.makeText(this@MainActivity, "Account incomplete or network error. Please register again.", android.widget.Toast.LENGTH_LONG).show()
                        authRepository.logout()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            } else {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    }
}
