package com.example.grammawastetracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.ActivityLoginBinding
import com.example.grammawastetracker.ui.common.MainActivity
import com.example.grammawastetracker.utils.Constants
import com.example.grammawastetracker.utils.showToast
import com.google.android.material.tabs.TabLayout

class LoginActivity : com.example.grammawastetracker.ui.common.BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupListeners()
        setupObservers()
        setupLanguageSwitch()
    }

    private fun setupLanguageSwitch() {
        val currentLang = com.example.grammawastetracker.utils.LocaleHelper.getLanguage(this)
        binding.languageSwitch.text = if (currentLang == "kn") "EN | KN" else "KN | EN"
        
        binding.languageSwitch.setOnClickListener {
            val nextLang = if (currentLang == "kn") "en" else "kn"
            com.example.grammawastetracker.utils.LocaleHelper.setLocale(this, nextLang)
            
            // Recreate seamlessly
            val intent = intent
            finish()
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isLoginMode = tab?.position == 0
                updateUIForMode()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateUIForMode() {
        if (isLoginMode) {
            binding.nameLayout.visibility = View.GONE
            binding.roleGroup.visibility = View.GONE
            binding.actionButton.text = getString(R.string.login)
        } else {
            binding.nameLayout.visibility = View.VISIBLE
            binding.roleGroup.visibility = View.VISIBLE
            binding.actionButton.text = getString(R.string.register)
        }
    }

    private fun setupListeners() {
        binding.actionButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pass = binding.passwordInput.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                showToast("Please fill all fields")
                return@setOnClickListener
            }

            if (isLoginMode) {
                viewModel.login(email, pass)
            } else {
                val name = binding.nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    showToast("Please enter your name")
                    return@setOnClickListener
                }
                val role = if (binding.radioResident.isChecked) Constants.ROLE_RESIDENT else Constants.ROLE_DRIVER
                viewModel.register(email, pass, name, role)
            }
        }
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { success ->
            if (success) {
                navigateToMain()
            } else {
                showToast("Login failed")
            }
        }

        viewModel.registerResult.observe(this) { error ->
            if (error == null) {
                showToast("Registration successful! Please log in.")
                binding.tabLayout.getTabAt(0)?.select()
                binding.passwordInput.text?.clear()
            } else {
                showToast(error)
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
