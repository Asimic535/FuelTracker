package com.example.fuel_tracker_app.ui.vehicles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.DialogRangeCalculatorBinding

class RangeCalculatorDialog : DialogFragment() {

    private var _binding: DialogRangeCalculatorBinding? = null
    private val binding get() = _binding!!

    private lateinit var vehicle: Vehicle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRangeCalculatorBinding.inflate(inflater, container, false)
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
            binding.etFuelAmount.isEnabled = false
            binding.btnCalculate.isEnabled = false
            binding.tilFuelAmount.helperText = "Vehicle consumption not defined"
        }
    }

    // Podešavanje click listener-a za dugmad
    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCalculate.setOnClickListener {
            calculateRange()
        }

        // Kalkulacija u realnom vremenu kada korisnik unosi količinu goriva
        binding.etFuelAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                calculateRange()
            }
        }
    }

    // Kalkulacija dosega vozila na osnovu količine goriva
    private fun calculateRange() {
        val fuelAmountText = binding.etFuelAmount.text.toString().trim()

        if (fuelAmountText.isEmpty()) {
            binding.tvResult.visibility = View.GONE
            binding.tilFuelAmount.error = null
            return
        }

        val fuelAmount = fuelAmountText.toDoubleOrNull()
        if (fuelAmount == null || fuelAmount <= 0) {
            binding.tilFuelAmount.error = "Unesite ispravnu količinu"
            binding.tvResult.visibility = View.GONE
            return
        }

        if (fuelAmount > 200) {
            binding.tilFuelAmount.error = "Maximum fuel amount is 200L"
            binding.tvResult.visibility = View.GONE
            return
        }

        binding.tilFuelAmount.error = null

        // Izračunaj doseg vozila
        val rangeKm = if (vehicle.fuelConsumption > 0) {
            (fuelAmount * 100) / vehicle.fuelConsumption
        } else {
            0.0
        }

        // Prikaži rezultat
        val resultText = buildString {
            append("With ")
            append(String.format("%.1f", fuelAmount))
            append(" liters of fuel, you can travel:\n\n")
            append("🗺️ Range: ")
            append(String.format("%.0f", rangeKm))
            append(" kilometers\n")
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
        const val TAG = "RangeCalculatorDialog"
        private const val ARG_VEHICLE = "vehicle"

        fun newInstance(vehicle: Vehicle): RangeCalculatorDialog {
            return RangeCalculatorDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_VEHICLE, vehicle)
                }
            }
        }
    }
}