package com.example.fuel_tracker_app.ui.vehicles

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fuel_tracker_app.R
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.ItemVehicleBinding

class VehiclesAdapter(
    private val onEditClick: (Vehicle) -> Unit,
    private val onDeleteClick: (Vehicle) -> Unit,
    private val onToggleVisibilityClick: (Vehicle) -> Unit,
    private val onQuickRefuelClick: (Vehicle) -> Unit,
    private val onCalculateFuelClick: (Vehicle) -> Unit,
    private val onCalculateRangeClick: (Vehicle) -> Unit,
    private val currentUserId: String
) : ListAdapter<Vehicle, VehiclesAdapter.VehicleViewHolder>(VehicleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = ItemVehicleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VehicleViewHolder(
        private val binding: ItemVehicleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vehicle: Vehicle) {
            val context = binding.root.context
            val isOwnedByUser = vehicle.isOwnedBy(currentUserId)

            // Osnovne informacije o vozilu
            binding.tvMakeModel.text = "${vehicle.make} ${vehicle.model}"

            // Detalji vozila u zasebnim linijama
            binding.tvVehicleYear.text = vehicle.year.toString()
            binding.tvVehiclePower.text = vehicle.getPowerDisplay()
            binding.tvFuelType.text = vehicle.fuelType

            // Potrošnja goriva - sakrivanje cijele kartice ako nema potrošnje
            if (vehicle.fuelConsumption > 0) {
                binding.consumptionCard.visibility = View.VISIBLE
                binding.tvConsumption.text = "${vehicle.fuelConsumption} L/100km"
            } else {
                binding.consumptionCard.visibility = View.GONE
            }

            // Vidljivost akcijskih dugmića
            if (isOwnedByUser) {
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE

                binding.btnEdit.setOnClickListener {
                    onEditClick(vehicle)
                }

                binding.btnDelete.setOnClickListener {
                    showDeleteConfirmation(vehicle)
                }
            } else {
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            }

            // Quick action dugmići - dostupni samo za korisnička vozila
            if (isOwnedByUser) {
                // Sve akcije su dostupne za korisnička vozila
                binding.btnQuickRefuel.setOnClickListener {
                    onQuickRefuelClick(vehicle)
                }

                binding.btnCalculateFuel.setOnClickListener {
                    if (vehicle.fuelConsumption > 0) {
                        onCalculateFuelClick(vehicle)
                    } else {
                        showNoConsumptionDialog()
                    }
                }

                binding.btnCalculateRange.setOnClickListener {
                    if (vehicle.fuelConsumption > 0) {
                        onCalculateRangeClick(vehicle)
                    } else {
                        showNoConsumptionDialog()
                    }
                }
            } else {
                // Za tuđa vozila, onemogući akcije
                binding.btnQuickRefuel.setOnClickListener {
                    showNotOwnerDialog("add fuel to")
                }

                binding.btnCalculateFuel.setOnClickListener {
                    showNotOwnerDialog("calculate fuel for")
                }

                binding.btnCalculateRange.setOnClickListener {
                    showNotOwnerDialog("calculate range for")
                }
            }
        }

        private fun showDeleteConfirmation(vehicle: Vehicle) {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_vehicle_title))
                .setMessage(
                    context.getString(
                        R.string.delete_vehicle_message,
                        vehicle.make,
                        vehicle.model
                    )
                )
                .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                    onDeleteClick(vehicle)
                }
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show()
        }

        private fun showNoConsumptionDialog() {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle("Fuel Consumption Required")
                .setMessage("Please set fuel consumption for this vehicle first.")
                .setPositiveButton("OK", null)
                .show()
        }

        private fun showNotOwnerDialog(action: String) {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle("Permission Required")
                .setMessage("You can only $action your own vehicles.")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}

class VehicleDiffCallback : DiffUtil.ItemCallback<Vehicle>() {
    override fun areItemsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean {
        return oldItem == newItem
    }
}