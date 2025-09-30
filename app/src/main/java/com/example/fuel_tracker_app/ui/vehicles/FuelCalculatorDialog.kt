package com.example.fuel_tracker_app.ui.vehicles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.DialogFuelCalculatorBinding

class FuelCalculatorDialog : DialogFragment() {

    private var _binding: DialogFuelCalculatorBinding? = null
    private val binding get() = _binding!!

    private lateinit var vehicle: Vehicle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFuelCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVehicleInfo()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Podešavanje širine dijaloga
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Podešavanje informacija o vozilu
    private fun setupVehicleInfo() {
        binding.tvVehicleInfo.text = "${vehicle.getDisplayTitle()}\n${vehicle.getConsumptionDisplay()}"

        // Ako vozilo nema definiranu potrošnju, onemogući kalkulaciju
        if (vehicle.fuelConsumption <= 0) {
            binding.etDistance.isEnabled = false
            binding.btnCalculate.isEnabled = false
            binding.tilDistance.helperText = "Vehicle consumption not defined"
        }
    }

    // Podešavanje click listener-a za dugmad
    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCalculate.setOnClickListener {
            calculateFuel()
        }

        // Kalkulacija u realnom vremenu kada korisnik unosi kilometražu
        binding.etDistance.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                calculateFuel()
            }
        }
    }

    // Kalkulacija potrebne količine goriva
    private fun calculateFuel() {
        val distanceText = binding.etDistance.text.toString().trim()

        if (distanceText.isEmpty()) {
            binding.tvResult.visibility = View.GONE
            binding.tilDistance.error = null
            return
        }

        val distance = distanceText.toDoubleOrNull()
        if (distance == null || distance <= 0) {
            binding.tilDistance.error = "Unesite validnu kilometražu"
            binding.tvResult.visibility = View.GONE
            return
        }

        if (distance > 10000) {
            binding.tilDistance.error = "Maksimalna kilometraža je 10,000 km"
            binding.tvResult.visibility = View.GONE
            return
        }

        binding.tilDistance.error = null

        // Izračunaj potrebno gorivo
        val fuelNeeded = vehicle.calculateFuelNeeded(distance)

        // Prikaži rezultat
        val resultText = buildString {
            append("For ")
            append(String.format("%.0f", distance))
            append(" km you need:\n\n")
            append("🛣️ Fuel: ")
            append(String.format("%.1f", fuelNeeded))
            append(" liters\n")
            append("⛽ Consumption: ")
            append(vehicle.getConsumptionDisplay())
        }

        binding.tvResult.text = resultText
        binding.tvResult.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            vehicle = args.getParcelable(ARG_VEHICLE) ?: Vehicle()
        }
    }

    companion object {
        const val TAG = "FuelCalculatorDialog"
        private const val ARG_VEHICLE = "vehicle"

        fun newInstance(vehicle: Vehicle): FuelCalculatorDialog {
            return FuelCalculatorDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_VEHICLE, vehicle)
                }
            }
        }
    }
}