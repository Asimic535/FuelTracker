package com.example.fuel_tracker_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuel_tracker_app.data.model.UserProfile
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.data.repo.FirebaseRepository
import com.example.fuel_tracker_app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class VehiclesViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // Lista korisničkih vozila
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _operationState = MutableStateFlow<Result<String>?>(null)
    val operationState: StateFlow<Result<String>?> = _operationState


    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private var vehiclesJob: Job? = null

    init {
        loadUserProfile()
        loadVehicles()
    }

    // Učitavanje profila korisnika
    private fun loadUserProfile() {
        val uid = repository.currentUserId() ?: return
        viewModelScope.launch {
            when (val result = repository.getUserProfile(uid)) {
                is Result.Success -> _userProfile.value = result.data
                is Result.Error -> _operationState.value = Result.Error(Exception("Error loading profile: ${result.exception.message}"))
                else -> {}
            }
        }
    }

    // Učitavanje SAMO korisničkih vozila
    fun loadVehicles() {
        val uid = repository.currentUserId() ?: return

        // Prekidanje prethodnog posla radi sprečavanja curenja memorije
        vehiclesJob?.cancel()

        vehiclesJob = viewModelScope.launch {
            repository.streamMyVehicles(uid)
                .catch { exception ->
                    _operationState.value = Result.Error(Exception("Error loading vehicles: ${exception.message}"))
                }.collect { vehiclesList ->
                    _vehicles.value = vehiclesList
                }
        }
    }

    // Dodavanje novog vozila sa informacijama o vlasniku
    fun addVehicle(vehicle: Vehicle) {
        val uid = repository.currentUserId() ?: return
        val userProfile = _userProfile.value

        viewModelScope.launch {
            _operationState.value = Result.Loading

            val vehicleWithOwner = vehicle.copy(
                ownerId = uid,
                ownerName = userProfile?.name ?: "Unknown User",
                isPublic = false
            )

            val result = repository.addVehicle(vehicleWithOwner)
            _operationState.value = when (result) {
                is Result.Success -> Result.Success("Vehicle added successfully")
                is Result.Error -> result
                else -> null
            }
        }
    }

    // Ažuriranje postojećeg vozila - čuvanje vlasništva
    fun updateVehicle(vehicle: Vehicle) {
        val uid = repository.currentUserId() ?: return
        android.util.Log.d("VehiclesViewModel", "updateVehicle called - vehicleId: ${vehicle.id}, userId: $uid")

        viewModelScope.launch {
            _operationState.value = Result.Loading

            val existingVehicleResult = repository.getVehicleById(vehicle.id)
            when (existingVehicleResult) {
                is Result.Success -> {
                    val existingVehicle = existingVehicleResult.data
                    if (existingVehicle == null) {
                        _operationState.value = Result.Error(Exception("Vehicle not found"))
                        return@launch
                    }

                    val updatedVehicle = vehicle.copy(
                        ownerId = existingVehicle.ownerId, // Zadrži postojećeg vlasnika
                        ownerName = existingVehicle.ownerName, // Zadrži ime vlasnika
                        isPublic = existingVehicle.isPublic, // Zadrži vidljivost
                        createdAt = existingVehicle.createdAt // Zadrži vreme kreiranja
                    )

                    val result = repository.updateVehicle(updatedVehicle, uid)
                    android.util.Log.d("VehiclesViewModel", "updateVehicle result: $result")
                    _operationState.value = when (result) {
                        is Result.Success -> Result.Success("Vehicle updated successfully")
                        is Result.Error -> result
                        else -> null
                    }
                }
                is Result.Error -> {
                    _operationState.value = Result.Error(existingVehicleResult.exception)
                }
                else -> {
                    _operationState.value = Result.Error(Exception("Failed to get vehicle"))
                }
            }
        }
    }

    // Brisanje vozila (samo vlasnik može da obriše)
    fun deleteVehicle(vehicleId: String) {
        val uid = repository.currentUserId() ?: return
        android.util.Log.d("VehiclesViewModel", "deleteVehicle called - vehicleId: $vehicleId, userId: $uid")

        viewModelScope.launch {
            _operationState.value = Result.Loading
            val result = repository.deleteVehicle(vehicleId, uid)
            android.util.Log.d("VehiclesViewModel", "deleteVehicle result: $result")
            _operationState.value = when (result) {
                is Result.Success -> Result.Success("Vehicle deleted successfully")
                is Result.Error -> result
                else -> null
            }
        }
    }

    // Dobijanje vozila po ID-ju
    suspend fun getVehicleById(vehicleId: String): Result<Vehicle?> {
        android.util.Log.d("VehiclesViewModel", "getVehicleById called with ID: $vehicleId")
        return repository.getVehicleById(vehicleId)
    }

    // Dobijanje ID-ja trenutnog korisnika
    fun getCurrentUserId(): String? = repository.currentUserId()

    // Resetovanje stanja operacije
    fun resetOperationState() {
        _operationState.value = null
    }
}