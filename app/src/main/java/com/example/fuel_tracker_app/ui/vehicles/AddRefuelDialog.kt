package com.example.fuel_tracker_app.ui.vehicles

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.fuel_tracker_app.R
import com.example.fuel_tracker_app.data.model.Refuel
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.DialogAddRefuelBinding
import com.example.fuel_tracker_app.ui.viewmodel.HomeViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddRefuelDialog : DialogFragment() {
    private var _binding: DialogAddRefuelBinding? = null
    private val binding get() = _binding!!

    // Koristi HomeViewModel sa dosegom aktivnosti za dijeljenje podataka
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val args: AddRefuelDialogArgs by navArgs()

    private var vehicles: List<Vehicle> = emptyList()
    private var selectedDate: Date = Date()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // SharedPreferences za pamćenje poslednje cijene goriva
    private val PREFS_NAME = "fuel_tracker_prefs"
    private val LAST_FUEL_PRICE_KEY = "last_fuel_price"
    private val LAST_TOTAL_COST_KEY = "last_total_cost"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddRefuelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePicker()
        setupClickListeners()
        observeViewModel()
        loadLastValues()

        // Postavi početni datum na današnji dan
        binding.etDate.setText(dateFormat.format(selectedDate))
    }

    override fun onStart() {
        super.onStart()
        // Postavi širinu dijaloga
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Podešavanje birača datuma
    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    binding.etDate.setText(dateFormat.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // Podešavanje click listener-a za dugmad
    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveRefuel()
            }
        }
    }

    // Posmatraj ViewModel - JEDINSTVENI IZVOR ISTINE
    private fun observeViewModel() {
        lifecycleScope.launch {
            homeViewModel.vehicles.collect { vehiclesList ->
                vehicles = vehiclesList
                setupVehicleDropdown()
            }
        }

        lifecycleScope.launch {
            homeViewModel.errorState.collect { error ->
                if (error != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    homeViewModel.clearError()
                }
            }
        }
        binding.etTotalCost.doAfterTextChanged { recalc() }
        binding.etPricePerLiter.doAfterTextChanged { recalc() }
        binding.actvVehicle.doAfterTextChanged { recalc() }

    }

    private fun recalc() {
        val totalCost = binding.etTotalCost.text.toString().toDoubleOrNull()
        val pricePerLiter = binding.etPricePerLiter.text.toString().toDoubleOrNull()

        if (totalCost != null && totalCost > 0 && pricePerLiter != null && pricePerLiter > 0) {
            val calculatedLiters = totalCost / pricePerLiter
            binding.layoutCalculations.visibility = View.VISIBLE
            binding.tvCalculatedLiters.text = String.format(Locale.getDefault(), " %.2f L", calculatedLiters)

            // Izračunaj i prikaži procijenjeni domet
            val selectedVehicle = getSelectedVehicle()
            if (selectedVehicle != null && selectedVehicle.fuelConsumption > 0) {
                val estimatedRange = selectedVehicle.calculateEstimatedRange(calculatedLiters)
                binding.tvEstimatedRange.text = String.format(Locale.getDefault(), " ~%.0f km", estimatedRange)
                binding.tvRangeLabel.visibility = View.VISIBLE
                binding.tvEstimatedRange.visibility = View.VISIBLE
            } else {
                binding.tvRangeLabel.visibility = View.GONE
                binding.tvEstimatedRange.visibility = View.GONE
            }
        } else {
            binding.layoutCalculations.visibility = View.GONE
            binding.tvRangeLabel.visibility = View.GONE
            binding.tvEstimatedRange.visibility = View.GONE
        }
    }


    // Podešavanje dropdown liste za vozila
    private fun setupVehicleDropdown() {
        if (vehicles.isEmpty()) {
            binding.actvVehicle.setText(getString(R.string.no_vehicles))
            binding.btnSave.isEnabled = false
            return
        }

        val vehicleNames = vehicles.map { "${it.make} ${it.model} (${it.year})" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleNames)
        binding.actvVehicle.setAdapter(adapter)
        binding.btnSave.isEnabled = true

        // Unaprijed odaberi vozilo ako je prosljeđeno kroz argumente
        if (args.vehicleId.isNotEmpty()) {
            val vehicleIndex = vehicles.indexOfFirst { it.id == args.vehicleId }
            if (vehicleIndex >= 0) {
                binding.actvVehicle.setText(vehicleNames[vehicleIndex], false)
            }
        }
    }

    // Validacija unesenih podataka
    private fun validateInput(): Boolean {
        val totalCostText = binding.etTotalCost.text.toString()
        val vehicleSelection = binding.actvVehicle.text.toString()

        // Obriši prethodne greške
        binding.tilTotalCost.error = null
        binding.tilVehicle.error = null
        binding.tilPricePerLiter.error = null

        // Validacija ukupne cijene
        if (totalCostText.isBlank()) {
            binding.tilTotalCost.error = getString(R.string.field_required)
            return false
        }

        val totalCost = totalCostText.toDoubleOrNull()
        if (totalCost == null || totalCost <= 0) {
            binding.tilTotalCost.error = getString(R.string.invalid_number)
            return false
        }

        if (totalCost > 10000) {
            binding.tilTotalCost.error = getString(R.string.max_total_cost)
            return false
        }

        // Validacija vozila
        if (vehicleSelection.isBlank()) {
            binding.tilVehicle.error = getString(R.string.field_required)
            return false
        }

        val selectedVehicle = getSelectedVehicle()
        if (selectedVehicle == null) {
            binding.tilVehicle.error = getString(R.string.select_valid_vehicle)
            return false
        }

        // Validacija cijene po litru
        val pricePerLiterText = binding.etPricePerLiter.text.toString()
        if (pricePerLiterText.isBlank()) {
            binding.tilPricePerLiter.error = getString(R.string.field_required)
            return false
        }

        val pricePerLiter = pricePerLiterText.toDoubleOrNull()
        if (pricePerLiter == null || pricePerLiter <= 0) {
            binding.tilPricePerLiter.error = getString(R.string.invalid_number)
            return false
        }

        return true
    }

    // Dobij odabrano vozilo
    private fun getSelectedVehicle(): Vehicle? {
        val vehicleSelection = binding.actvVehicle.text.toString()
        return vehicles.find { vehicle ->
            "${vehicle.make} ${vehicle.model} (${vehicle.year})" == vehicleSelection
        }
    }

    // Sačuvaj točenje
    private fun saveRefuel() {
        val selectedVehicle = getSelectedVehicle() ?: return
        val totalCost = binding.etTotalCost.text.toString().toDoubleOrNull() ?: return
        val pricePerLiter = binding.etPricePerLiter.text.toString().toDoubleOrNull() ?: return

        // Izračunaj količinu litara iz ukupne cijene i cijene po litru
        val calculatedAmount = totalCost / pricePerLiter

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val refuel = Refuel(
            vehicleId = selectedVehicle.id,
            amount = calculatedAmount,
            pricePerLiter = pricePerLiter,
            date = Timestamp(selectedDate)
        )

        homeViewModel.addRefuel(refuel)

        // Sačuvaj poslijednje vrijednosti za sljedeći put
        saveLastValues(totalCost, pricePerLiter)

        // Zatvori dijalog nakon uspiješnog čuvanja
        lifecycleScope.launch {
            // Sačekaj trenutak da se operacija završi
            kotlinx.coroutines.delay(500)
            if (homeViewModel.errorState.value == null) {
                dismiss()
            }
        }
    }

    // Učitaj poslednje vrijednosti iz SharedPreferences
    private fun loadLastValues() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastPrice = prefs.getFloat(LAST_FUEL_PRICE_KEY, 0f)
        val lastTotalCost = prefs.getFloat(LAST_TOTAL_COST_KEY, 0f)

        if (lastPrice > 0) {
            binding.etPricePerLiter.setText(String.format(Locale.getDefault(), "%.2f", lastPrice))
        }

        if (lastTotalCost > 0) {
            binding.etTotalCost.setText(String.format(Locale.getDefault(), "%.2f", lastTotalCost))
        }
    }

    // Sačuvaj posljednje vrijednosti u SharedPreferences
    private fun saveLastValues(totalCost: Double, pricePerLiter: Double) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(LAST_FUEL_PRICE_KEY, pricePerLiter.toFloat())
            .putFloat(LAST_TOTAL_COST_KEY, totalCost.toFloat())
            .apply()
    }

    // Obriši sačuvane podatke (poziva se pri odjavi)
    companion object {
        fun clearSavedData(context: Context) {
            val prefs = context.getSharedPreferences("fuel_tracker_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}