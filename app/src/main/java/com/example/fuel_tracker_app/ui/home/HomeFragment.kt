package com.example.fuel_tracker_app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fuel_tracker_app.R
import com.example.fuel_tracker_app.ui.vehicles.RangeCalculatorDialog
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.FragmentHomeBinding
import com.example.fuel_tracker_app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var refuelAdapter: RefuelAdapter

    // Lista opcija perioda za filtriranje
    private val periodOptions = listOf("all-time", "month", "year")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupClickListeners()
        observeViewModel()
    }

    // Podešavanje RecyclerView-a za prikaz tankovanja
    private fun setupRecyclerView() {
        refuelAdapter = RefuelAdapter()
        binding.rvRefuels.apply {
            adapter = refuelAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // Podešavanje spinner-a za filtriranje po periodu i vozilu
    private fun setupFilters() {
        // Period spinner
        val periodAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_dark,
            periodOptions.map { formatPeriodText(it) }
        )
        periodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        binding.spinnerPeriod.adapter = periodAdapter

        // Postavi početnu selekciju na "Od početka"
        binding.spinnerPeriod.setSelection(0)

        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPeriod = periodOptions[position]
                viewModel.updatePeriodFilter(selectedPeriod)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Podešavanje click listener-a za dugmad i UI elemente
    private fun setupClickListeners() {
        binding.fabAddRefuel.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_add_refuel)
        }

        binding.btnCalculateRange.setOnClickListener {
            showRangeCalculatorDialog()
        }
    }

    // Prikaži dialog za izračunavanje dosega vozila
    private fun showRangeCalculatorDialog() {
        val vehicles = viewModel.vehicles.value
        if (vehicles.isEmpty()) {
            Toast.makeText(requireContext(), "You don't have any vehicles yet", Toast.LENGTH_SHORT).show()
            return
        }

        // Za sada prikaži dialog sa prvim vozilom, kasnije možemo dodati selektor vozila
        val firstVehicle = vehicles.first()
        val dialog = RangeCalculatorDialog.newInstance(firstVehicle)
        dialog.show(parentFragmentManager, RangeCalculatorDialog.TAG)
    }

    // Posmatraj promene u ViewModel-u i ažuriraj UI
    private fun observeViewModel() {
        // Posmatraj promene u profilu korisnika i prikaži dobrodošlicu
        lifecycleScope.launch {
            viewModel.userProfile.collect { profile ->
                profile?.let {
                    binding.tvWelcome.text = getString(R.string.welcome, it.name)
                }
            }
        }

        // Posmatraj listu vozila i ažuriraj spinner za filtriranje
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vehicles.collect { vehicles ->
                if (_binding == null) return@collect
                setupVehicleSpinner(vehicles)
                refuelAdapter.updateVehicles(vehicles)
            }
        }

        // Posmatraj listu tankovanja i prikaži ih u RecyclerView-u
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.refuels.collect { refuels ->
                if (_binding == null) return@collect
                refuelAdapter.submitList(refuels)

                // Prikaži/sakrij empty state
                if (refuels.isEmpty()) {
                    binding.rvRefuels.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvRefuels.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
            }
        }

        // Posmatraj ukupnu potrošenu količinu goriva i prikaži statistike
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalFuel.collect { total ->
                if (_binding == null) return@collect
                binding.tvTotalFuel.text = "${String.format("%.1f", total)} L"
            }
        }

        // Posmatraj ukupne troškove
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalCost.collect { total ->
                if (_binding == null) return@collect
                 binding.tvTotalCost.text = "${String.format("%.2f", total)} KM"
            }
        }
    }

    // Podešavanje spinner-a za filtriranje po vozilu
    private fun setupVehicleSpinner(vehicles: List<Vehicle>) {
        val vehicleOptions = mutableListOf(getString(R.string.all_vehicles))
        vehicleOptions.addAll(vehicles.map { "${it.make} ${it.model}" })

        val vehicleAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_dark,
            vehicleOptions
        )
        vehicleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        binding.spinnerVehicle.adapter = vehicleAdapter

        binding.spinnerVehicle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedVehicleId = if (position == 0) {
                    null // Sva vozila
                } else {
                    vehicles.getOrNull(position - 1)?.id
                }
                viewModel.updateVehicleFilter(selectedVehicleId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Formatiranje teksta za prikaz vremenskog perioda
    private fun formatPeriodText(period: String): String {
        return when (period) {
            "all-time" -> getString(R.string.all_time)
            "month" -> getString(R.string.this_month)
            "year" -> getString(R.string.this_year)
            else -> period
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}