package com.example.fuel_tracker_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fuel_tracker_app.data.repo.FirebaseRepository

class SettingsViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // Izlogiraj korisnika
    fun logout() {
        repository.authSignOut()
    }
}