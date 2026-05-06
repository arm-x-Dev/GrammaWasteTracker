package com.example.grammawastetracker.ui.driver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.ActivityDriverDashboardBinding
import com.example.grammawastetracker.utils.startForestGlowAnimation

class DriverDashboardActivity : com.example.grammawastetracker.ui.common.BaseActivity() {

    private lateinit var binding: ActivityDriverDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Start subtle forest glow aura (bleeding light effect)
            binding.topBarAura.startForestGlowAnimation()
            
            // Set toggle text based on current locale
            val currentLang = com.example.grammawastetracker.utils.LocaleHelper.getLanguage(this)
            binding.languageSwitch.text = if (currentLang == "kn") "EN | KN" else "KN | EN"

            binding.bottomNavigation.setOnItemSelectedListener { item ->
                // Keep top bar visible on all tabs for consistency
                binding.topBar.visibility = android.view.View.VISIBLE

                when (item.itemId) {
                    R.id.driverReportsFragment -> {
                        loadFragment(DriverReportsFragment())
                        true
                    }
                    R.id.driverLocationFragment -> {
                        loadFragment(DriverLocationFragment())
                        true
                    }
                    R.id.driverCompletedFragment -> {
                        loadFragment(DriverCompletedFragment())
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

            // Fetch real name from database
            lifecycleScope.launchWhenStarted {
                val authRepo = com.example.grammawastetracker.data.repository.AuthRepository(com.example.grammawastetracker.data.remote.FirebaseService())
                val userDetails = authRepo.getUserDetails()
                binding.navigationView.findViewById<android.widget.TextView>(R.id.user_name_text)?.text = 
                    userDetails?.name ?: currentUser?.displayName ?: getString(R.string.default_driver_name)
            }

            binding.navigationView.findViewById<android.view.View>(R.id.sign_out_button)?.setOnClickListener {
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
                binding.bottomNavigation.selectedItemId = R.id.driverReportsFragment
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Init Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
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
