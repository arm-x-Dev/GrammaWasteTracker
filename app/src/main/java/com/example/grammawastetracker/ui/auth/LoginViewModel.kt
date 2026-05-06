package com.example.grammawastetracker.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grammawastetracker.data.remote.FirebaseService
import com.example.grammawastetracker.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authRepository = AuthRepository(FirebaseService())

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _registerResult = MutableLiveData<String?>()
    val registerResult: LiveData<String?> = _registerResult

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _loginResult.value = authRepository.login(email, pass)
        }
    }

    fun register(email: String, pass: String, name: String, role: String) {
        viewModelScope.launch {
            _registerResult.value = authRepository.register(email, pass, name, role)
        }
    }
}
