package com.example.fuel_tracker_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuel_tracker_app.data.repo.FirebaseRepository
import com.example.fuel_tracker_app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _loginState = MutableStateFlow<Result<String>?>(null)
    val loginState: StateFlow<Result<String>?> = _loginState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Prijavi korisnika
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginState.value = Result.Loading

            val result = repository.authSignIn(email, password)
            _loginState.value = result
            _isLoading.value = false
        }
    }

    // Resetiraj stanje
    fun resetState() {
        _loginState.value = null
    }
}