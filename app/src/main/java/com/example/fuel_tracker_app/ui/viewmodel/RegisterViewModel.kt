package com.example.fuel_tracker_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuel_tracker_app.data.repo.FirebaseRepository
import com.example.fuel_tracker_app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _registerState = MutableStateFlow<Result<String>?>(null)
    val registerState: StateFlow<Result<String>?> = _registerState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Registriraj novog korisnika
    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _registerState.value = Result.Loading

            val result = repository.authSignUp(name, email, password)
            _registerState.value = result
            _isLoading.value = false
        }
    }

    // Resetiraj stanje
    fun resetState() {
        _registerState.value = null
    }
}