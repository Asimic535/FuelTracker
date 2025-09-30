package com.example.fuel_tracker_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuel_tracker_app.data.model.Refuel
import com.example.fuel_tracker_app.data.model.UserProfile
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.data.repo.FirebaseRepository
import com.example.fuel_tracker_app.util.DateExt
import com.example.fuel_tracker_app.util.Result
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class FilterState(
    val period: String = "all-time", // Zadati filter perioda
    val selectedVehicleId: String? = null, // null znači "Sva vozila"
    val showOnlyMyRefuels: Boolean = true // prikazuj samo korisnikova točenja
)

class HomeViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    private val _refuels = MutableStateFlow<List<Refuel>>(emptyList())
    val refuels: StateFlow<List<Refuel>> = _refuels

    private val _totalFuel = MutableStateFlow(0.0)
    val totalFuel: StateFlow<Double> = _totalFuel

    // Dodaj praćenje ukupnih troškova
    private val _totalCost = MutableStateFlow(0.0)
    val totalCost: StateFlow<Double> = _totalCost

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    private var refuelsJob: Job? = null
    private var vehiclesJob: Job? = null

    init {
        loadUserData()
    }

    // Učitavanje svih korisničkih podataka
    private fun loadUserData() {
        val uid = repository.currentUserId() ?: return

        viewModelScope.launch {
            // Učitavanje profila korisnika iz Firebase-a
            when (val result = repository.getUserProfile(uid)) {
                is Result.Success -> _userProfile.value = result.data
                is Result.Error -> _errorState.value = "Error loading profile: ${result.exception.message}"
                else -> {}
            }

            // Prekidanje prethodnog posla za vozila radi sprječavanja curenja memorije
            vehiclesJob?.cancel()

            vehiclesJob = launch {
                // Učitavanje samo korisničkih vozila
                repository.streamMyVehicles(uid)
                    .catch { exception ->
                        _errorState.value = "Error loading vehicles: ${exception.message}"
                    }
                    .collect { vehiclesList ->
                        _vehicles.value = vehiclesList
                    }
            }
        }

        // Učitavanje točenja na osnovu trenutnog filtera
        loadRefuelsWithFilter()
    }

    // Učitavanje točenja na osnovu trenutno postavljenog filtera
    private fun loadRefuelsWithFilter() {
        val uid = repository.currentUserId() ?: return

        // Prekidanje prethodnog posla radi sprječavanja curenja memorije
        refuelsJob?.cancel()

        refuelsJob = viewModelScope.launch {
            combine(
                _filterState,
                repository.streamRefuelsFiltered(
                    currentUserId = uid,
                    vehicleIdOrNull = _filterState.value.selectedVehicleId,
                    fromDateOrNull = getStartDateForPeriod(_filterState.value.period),
                    toDateOrNull = getEndDateForPeriod(_filterState.value.period),
                    showOnlyMyRefuels = _filterState.value.showOnlyMyRefuels
                )
            ) { _, refuelsList ->
                refuelsList
            }
                .catch { exception ->
                    _errorState.value = "Error loading refuels: ${exception.message}"
                }
                .collect { refuelsList ->
                    _refuels.value = refuelsList
                    _totalFuel.value = refuelsList.sumOf { it.amount }
                    // Izračunaj ukupne troškove
                    _totalCost.value = refuelsList.sumOf { it.totalCost }
                }
        }
    }

    // Ažuriranje filtera za vremenski period (mjesec, godina, svo vrijeme)
    fun updatePeriodFilter(period: String) {
        _filterState.value = _filterState.value.copy(period = period)
        loadRefuelsWithFilter()
    }

    // Ažuriranje filtera za vozilo (prikaz točenja za određeno vozilo)
    fun updateVehicleFilter(vehicleId: String?) {
        _filterState.value = _filterState.value.copy(selectedVehicleId = vehicleId)
        loadRefuelsWithFilter()
    }

    // Prebacivanje između prikaza samo mojih točenja ili svih točenja
    fun toggleRefuelOwnerFilter() {
        _filterState.value = _filterState.value.copy(
            showOnlyMyRefuels = !_filterState.value.showOnlyMyRefuels
        )
        loadRefuelsWithFilter()
    }

    // Dobijanje početnog datuma za izabrani vremenski period
    private fun getStartDateForPeriod(period: String): Timestamp? {
        return when (period) {
            "month" -> DateExt.getStartOfCurrentMonth()
            "year" -> DateExt.getStartOfCurrentYear()
            else -> null // sve vreme
        }
    }

    // Dobijanje završnog datuma za izabrani vremenski period
    private fun getEndDateForPeriod(period: String): Timestamp? {
        return when (period) {
            "month" -> DateExt.getEndOfCurrentMonth()
            "year" -> DateExt.getEndOfCurrentYear()
            else -> null // sve vreme
        }
    }

    // Dodavanje novog točenja sa proširenim podacima
    fun addRefuel(refuel: Refuel) {
        val uid = repository.currentUserId() ?: return
        val userProfile = _userProfile.value

        viewModelScope.launch {
            // Dobijanje informacija o vozilu za keširane i kalkulaciju dosega
            when (val vehicleResult = repository.getVehicleById(refuel.vehicleId)) {
                is Result.Success -> {
                    val vehicle = vehicleResult.data

                    // Izračunaj doseg ako vozilo ima definiranu potrošnju
                    val estimatedRange = if (vehicle?.fuelConsumption != null && vehicle.fuelConsumption > 0) {
                        Refuel.calculateEstimatedRange(refuel.amount, vehicle.fuelConsumption)
                    } else {
                        0.0
                    }

                    val refuelWithCompleteData = refuel.copy(
                        ownerId = uid,
                        ownerName = userProfile?.name ?: "Unknown User",
                        vehicleMake = vehicle?.make ?: "",
                        vehicleModel = vehicle?.model ?: "",
                        vehicleFuelConsumption = vehicle?.fuelConsumption ?: 0.0,
                        estimatedRange = estimatedRange, // Dodaj izračunat doseg
                        totalCost = Refuel.calculateTotalCost(refuel.amount, refuel.pricePerLiter) // Osiguraj da je ukupna cena izračunata
                    )

                    when (val result = repository.addRefuel(refuelWithCompleteData)) {
                        is Result.Error -> _errorState.value = "Error adding refuel: ${result.exception.message}"
                        else -> {} // Uspijeh će biti automatski prikazan kroz stream
                    }
                }
                is Result.Error -> _errorState.value = "Error getting vehicle info: ${vehicleResult.exception.message}"
                else -> {}
            }
        }
    }

    // Dobijanje ID-ja trenutno ulogiranog korisnika
    fun getCurrentUserId(): String? = repository.currentUserId()

    // Čišćenje greške iz stanja
    fun clearError() {
        _errorState.value = null
    }
}