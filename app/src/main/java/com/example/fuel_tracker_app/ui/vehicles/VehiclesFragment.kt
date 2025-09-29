package com.example.fuel_tracker_app.ui.vehicles

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fuel_tracker_app.R
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.FragmentVehiclesBinding
import com.example.fuel_tracker_app.ui.viewmodel.VehiclesViewModel
import com.example.fuel_tracker_app.util.Result
import kotlinx.coroutines.launch

class VehiclesFragment : Fragment() {
    private var _binding: FragmentVehiclesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VehiclesViewModel by viewModels()
    private lateinit var vehiclesAdapter: VehiclesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    // Podešavanje RecyclerView-a sa adapterom za prikaz vozila
    private fun setupRecyclerView() {
        val currentUserId = viewModel.getCurrentUserId() ?: ""

        vehiclesAdapter = VehiclesAdapter(
            onEditClick = { vehicle ->
                // Svi prikazani vozila su korisnikova, pa uvek dozvoli edit
                val bundle = bundleOf("vehicleId" to vehicle.id)
                findNavController().navigate(R.id.action_vehicles_to_edit_vehicle, bundle)
            },
            onDeleteClick = { vehicle ->
                // Svi prikazani vozila su korisnikova, pa uvek dozvoli brisanje
                showDeleteConfirmDialog(vehicle)
            },
            onToggleVisibilityClick = { vehicle ->
                // Funkcionalnost vidljivosti je uklonjena
                Toast.makeText(requireContext(), "All vehicles are private by default", Toast.LENGTH_SHORT).show()
            },
            onQuickRefuelClick = { vehicle ->
                // Brzo dodavanje tankovanja
                val bundle = bundleOf("vehicleId" to vehicle.id)
                findNavController().navigate(R.id.addRefuelDialog, bundle)
            },
            onCalculateFuelClick = { vehicle ->
                // Prikaži dijalog za kalkulaciju potrebnog goriva
                showFuelCalculatorDialog(vehicle)
            },
            onCalculateRangeClick = { vehicle ->
                // Prikaži dijalog za kalkulaciju dosega vozila
                showRangeCalculatorDialog(vehicle)
            },
            currentUserId = currentUserId
        )

        binding.rvVehicles.apply {
            adapter = vehiclesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // Podešavanje click listener-a za dugmad i UI elemente
    private fun setupClickListeners() {

        // FAB dugme za dodavanje novog vozila
        binding.fabAddVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_vehicles_to_edit_vehicle)
        }

    }

    // Posmatraj promene u ViewModel-u i ažuriraj UI
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vehicles.collect { vehicles ->
                if (_binding == null) return@collect
                vehiclesAdapter.submitList(vehicles)

                // Prikaži/sakrij empty state na osnovu broja vozila
                if (vehicles.isEmpty()) {
                    binding.rvVehicles.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvVehicles.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }

                // Ažuriraj statistike vozila
                binding.tvTotalVehicles.text = vehicles.size.toString()
                binding.tvMyVehicles.text = vehicles.size.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.operationState.collect { result ->
                if (_binding == null) return@collect
                when (result) {
                    is Result.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Result.Success -> {
                        binding.progressBar.visibility = View.GONE
                        if (result.data.isNotEmpty()) {
                            Toast.makeText(requireContext(), result.data, Toast.LENGTH_SHORT).show()
                        }
                        viewModel.resetOperationState()
                    }
                    is Result.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Error: ${result.exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetOperationState()
                    }
                    null -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

    }

    // Prikaži dialog za potvrdu brisanja vozila
    private fun showDeleteConfirmDialog(vehicle: Vehicle) {
        android.util.Log.d("VehiclesFragment", "Showing delete confirmation for vehicle: ${vehicle.id}")
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_vehicle_title))
            .setMessage(getString(R.string.delete_vehicle_message, vehicle.make, vehicle.model))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                android.util.Log.d("VehiclesFragment", "User confirmed delete for vehicle: ${vehicle.id}")
                viewModel.deleteVehicle(vehicle.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // Prikaži dijalog za kalkulaciju potrebnog goriva
    private fun showFuelCalculatorDialog(vehicle: Vehicle) {
        val dialog = FuelCalculatorDialog.newInstance(vehicle)
        dialog.show(parentFragmentManager, FuelCalculatorDialog.TAG)
    }

    // Prikaži dijalog za kalkulaciju dosega vozila
    private fun showRangeCalculatorDialog(vehicle: Vehicle) {
        val dialog = RangeCalculatorDialog.newInstance(vehicle)
        dialog.show(parentFragmentManager, RangeCalculatorDialog.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}