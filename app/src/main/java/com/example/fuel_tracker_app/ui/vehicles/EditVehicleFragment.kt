package com.example.fuel_tracker_app.ui.vehicles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fuel_tracker_app.R
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.FragmentEditVehicleBinding
import com.example.fuel_tracker_app.ui.viewmodel.VehiclesViewModel
import com.example.fuel_tracker_app.util.Result
import kotlinx.coroutines.launch

class EditVehicleFragment : Fragment() {
    private var _binding: FragmentEditVehicleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VehiclesViewModel by viewModels()
    private val args: EditVehicleFragmentArgs by navArgs()

    private var currentVehicle: Vehicle? = null
    private val isEditMode: Boolean get() = args.vehicleId.isNotEmpty()

    // Lista tipova goriva
    private val fuelTypes by lazy {
        listOf(
            getString(R.string.gasoline),
            getString(R.string.diesel),
            getString(R.string.hybrid),
            getString(R.string.electric),
            getString(R.string.lpg),
            "CNG"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFuelTypeDropdown()
        setupPowerConversion()
        setupClickListeners()
        observeViewModel()

        // Podesi naslov na osnovu mode-a
        binding.tvTitle.text = if (isEditMode) getString(R.string.edit_vehicle_title) else getString(R.string.add_vehicle_title)

        if (isEditMode) {
            loadVehicleData()
        }
    }

    // Podesi dropdown za tip goriva
    private fun setupFuelTypeDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fuelTypes)
        binding.actvFuelType.setAdapter(adapter)
    }

    // Podesi real-time pretvorbu kW u konjske snage
    private fun setupPowerConversion() {
        binding.etPower.setOnFocusChangeListener { _, _ ->
            updateHorsepowerDisplay()
        }
    }

    // Ažuriraj prikaz konjskih snaga na osnovu unijetih kW
    private fun updateHorsepowerDisplay() {
        val powerText = binding.etPower.text.toString().trim()
        if (powerText.isNotEmpty()) {
            val powerKw = powerText.toDoubleOrNull()
            if (powerKw != null && powerKw > 0) {
                val horsepower = (powerKw * 1.36).toInt()
                binding.tilPower.helperText = "$horsepower HP"
            } else {
                binding.tilPower.helperText = "in kW"
            }
        } else {
            binding.tilPower.helperText = "in kW"
        }
    }

    // Podesi click listener-e za dugmad
    private fun setupClickListeners() {
        // Dugme za čuvanje vozila
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveVehicle()
            }
        }

        // Dugme za otkazivanje - vraća korisnika na prethodni ekran
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    // Posmatraj promene u ViewModel-u
    private fun observeViewModel() {
        if (isEditMode) {
            lifecycleScope.launch {
                viewModel.vehicles.collect { vehicles ->
                    if (currentVehicle == null) {
                        currentVehicle = vehicles.find { it.id == args.vehicleId }
                        currentVehicle?.let { populateFields(it) }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.operationState.collect { result ->
                when (result) {
                    is Result.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnSave.isEnabled = false
                    }
                    is Result.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            if (isEditMode) getString(R.string.vehicle_updated_successfully) else getString(R.string.vehicle_added_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetOperationState()
                        findNavController().navigateUp()
                    }
                    is Result.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error, result.exception.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetOperationState()
                    }
                    null -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                    }
                }
            }
        }
    }

    // Učitaj podatke vozila za edit mode
    private fun loadVehicleData() {
        if (isEditMode) {
            android.util.Log.d("EditVehicleFragment", "Loading vehicle data for ID: ${args.vehicleId}")
            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.getVehicleById(args.vehicleId)
                when (result) {
                    is Result.Success -> {
                        val vehicle = result.data
                        if (vehicle != null) {
                            android.util.Log.d("EditVehicleFragment", "Vehicle loaded successfully: ${vehicle.id}")
                            currentVehicle = vehicle
                            populateFields(vehicle)
                        } else {
                            android.util.Log.e("EditVehicleFragment", "Vehicle not found with ID: ${args.vehicleId}")
                            Toast.makeText(requireContext(), "Vehicle not found", Toast.LENGTH_LONG).show()
                            findNavController().navigateUp()
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e("EditVehicleFragment", "Error loading vehicle: ${result.exception.message}")
                        Toast.makeText(requireContext(), "Error loading vehicle: ${result.exception.message}", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                    else -> {
                        android.util.Log.e("EditVehicleFragment", "Unexpected result type")
                    }
                }
            }
        }
    }

    // Popuni polja sa podacima vozila
    private fun populateFields(vehicle: Vehicle) {
        binding.etMake.setText(vehicle.make)
        binding.etModel.setText(vehicle.model)
        binding.etYear.setText(vehicle.year.toString())
        binding.etPower.setText(vehicle.powerKw.toString())
        if (vehicle.fuelConsumption > 0) {
            binding.etFuelConsumption.setText(vehicle.fuelConsumption.toString())
        }
        binding.actvFuelType.setText(vehicle.fuelType, false)

        // Ažuriraj prikaz konjskih snaga
        updateHorsepowerDisplay()
    }

    // Validiraj unos
    private fun validateInput(): Boolean {
        var isValid = true

        val make = binding.etMake.text.toString().trim()
        if (make.isEmpty()) {
            binding.tilMake.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilMake.error = null
        }

        val model = binding.etModel.text.toString().trim()
        if (model.isEmpty()) {
            binding.tilModel.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilModel.error = null
        }

        val yearText = binding.etYear.text.toString().trim()
        if (yearText.isEmpty()) {
            binding.tilYear.error = getString(R.string.field_required)
            isValid = false
        } else {
            val year = yearText.toIntOrNull()
            if (year == null || year < 1900 || year > 2030) {
                binding.tilYear.error = getString(R.string.invalid_vehicle_year)
                isValid = false
            } else {
                binding.tilYear.error = null
            }
        }

        val powerText = binding.etPower.text.toString().trim()
        if (powerText.isEmpty()) {
            binding.tilPower.error = getString(R.string.field_required)
            isValid = false
        } else {
            val power = powerText.toDoubleOrNull()
            if (power == null || power <= 0) {
                binding.tilPower.error = getString(R.string.invalid_engine_power)
                isValid = false
            } else {
                binding.tilPower.error = null
            }
        }

        val fuelConsumptionText = binding.etFuelConsumption.text.toString().trim()
        if (fuelConsumptionText.isNotEmpty()) {
            val consumption = fuelConsumptionText.toDoubleOrNull()
            if (consumption == null || consumption <= 0 || consumption > 50) {
                binding.tilFuelConsumption.error = "Unesite validnu potrošnju (0-50 L/100km)"
                isValid = false
            } else {
                binding.tilFuelConsumption.error = null
            }
        } else {
            binding.tilFuelConsumption.error = null
        }

        val fuelType = binding.actvFuelType.text.toString().trim()
        if (fuelType.isEmpty()) {
            binding.tilFuelType.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilFuelType.error = null
        }

        return isValid
    }

    // Sačuvaj vozilo
    private fun saveVehicle() {
        val fuelConsumptionText = binding.etFuelConsumption.text.toString().trim()
        val fuelConsumption = if (fuelConsumptionText.isNotEmpty()) {
            fuelConsumptionText.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        val vehicle = Vehicle(
            id = if (isEditMode) args.vehicleId else "",
            make = binding.etMake.text.toString().trim(),
            model = binding.etModel.text.toString().trim(),
            year = binding.etYear.text.toString().toInt(),
            powerKw = binding.etPower.text.toString().toDouble(),
            fuelType = binding.actvFuelType.text.toString().trim(),
            fuelConsumption = fuelConsumption
        )

        if (isEditMode) {
            viewModel.updateVehicle(vehicle)
        } else {
            viewModel.addVehicle(vehicle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}