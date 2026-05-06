package com.example.grammawastetracker.ui.resident

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.ActivityResidentDashboardBinding
import com.example.grammawastetracker.utils.startForestGlowAnimation

class ResidentDashboardActivity : com.example.grammawastetracker.ui.common.BaseActivity() {

    private lateinit var binding: ActivityResidentDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityResidentDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Start subtle forest glow aura (bleeding light effect)
            binding.topBarAura.startForestGlowAnimation()
            
            // Set toggle text based on current locale
            val currentLang = com.example.grammawastetracker.utils.LocaleHelper.getLanguage(this)
            binding.languageSwitch.text = if (currentLang == "kn") "EN | KN" else "KN | EN"

            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.trackDriverFragment -> {
                        loadFragment(TrackDriverFragment())
                        true
                    }
                    R.id.uploadReportFragment -> {
                        loadFragment(UploadReportFragment())
                        true
                    }
                    R.id.residentHistoryFragment -> {
                        loadFragment(ResidentHistoryFragment())
                        true
                    }
                    else -> false
                }
            }
            
            binding.menuButton.setOnClickListener {
                binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
            }

            binding.navigationView.findViewById<android.view.View>(R.id.close_drawer_button)?.setOnClickListener {
                binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            }

            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            binding.navigationView.findViewById<android.widget.TextView>(R.id.user_email_text)?.text = 
                currentUser?.email ?: ""

            // Fetch real name from database and start background service
            lifecycleScope.launchWhenStarted {
                val authRepo = com.example.grammawastetracker.data.repository.AuthRepository(com.example.grammawastetracker.data.remote.FirebaseService())
                val userDetails = authRepo.getUserDetails()
                binding.navigationView.findViewById<android.widget.TextView>(R.id.user_name_text)?.text = 
                    userDetails?.name ?: currentUser?.displayName ?: getString(R.string.default_user_name)
                
                // Start background monitoring service
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this@ResidentDashboardActivity)
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            com.example.grammawastetracker.service.ResidentProximityService.start(
                                this@ResidentDashboardActivity,
                                location.latitude,
                                location.longitude
                            )
                        }
                    }
                } catch (e: SecurityException) { }
            }

            binding.navigationView.findViewById<android.view.View>(R.id.sign_out_button)?.setOnClickListener {
                com.example.grammawastetracker.service.ResidentProximityService.stop(this)
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                startActivity(android.content.Intent(this, com.example.grammawastetracker.ui.auth.LoginActivity::class.java))
                finishAffinity()
            }

            binding.languageSwitch.setOnClickListener {
                val currentLang = com.example.grammawastetracker.utils.LocaleHelper.getLanguage(this)
                val newLang = if (currentLang == "kn") "en" else "kn"
                
                com.example.grammawastetracker.utils.LocaleHelper.setLocale(this, newLang)
                
                // Save current tab and recreate seamlessly
                val currentTab = binding.bottomNavigation.selectedItemId
                val intent = intent
                intent.putExtra("selected_tab", currentTab)
                finish()
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            
            val savedTab = intent.getIntExtra("selected_tab", -1)
            if (savedTab != -1) {
                binding.bottomNavigation.selectedItemId = savedTab
            } else if (savedInstanceState == null) {
                binding.topBar.visibility = android.view.View.VISIBLE
                binding.bottomNavigation.selectedItemId = R.id.trackDriverFragment
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Init Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    fun switchToHistoryTab() {
        binding.bottomNavigation.selectedItemId = R.id.residentHistoryFragment
    }

    private fun loadFragment(fragment: Fragment) {
        val tag = fragment.javaClass.simpleName
        val currentFragment = supportFragmentManager.findFragmentByTag(tag)
        
        if (currentFragment != null && currentFragment.isVisible) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment, tag)
            .commit()
    }
}
