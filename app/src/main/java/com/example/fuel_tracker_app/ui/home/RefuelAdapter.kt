package com.example.fuel_tracker_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fuel_tracker_app.data.model.Refuel
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.databinding.ItemRefuelBinding
import java.text.SimpleDateFormat
import java.util.*

class RefuelAdapter : ListAdapter<Refuel, RefuelAdapter.RefuelViewHolder>(RefuelDiffCallback()) {

    private var vehicles: Map<String, Vehicle> = emptyMap()

    fun updateVehicles(vehiclesList: List<Vehicle>) {
        vehicles = vehiclesList.associateBy { it.id }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RefuelViewHolder {
        val binding = ItemRefuelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RefuelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RefuelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RefuelViewHolder(
        private val binding: ItemRefuelBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())


        fun bind(refuel: Refuel) {

            binding.tvAmount.text = String.format(Locale.getDefault(), "%.1f L", refuel.amount)

            binding.tvCost.text = String.format(Locale.getDefault(), "%.2f KM", refuel.totalCost)
            binding.tvCost.visibility = if (refuel.totalCost > 0) View.VISIBLE else View.GONE

            // Prikaži informacije o vozilu
            binding.tvVehicle.text = refuel.getVehicleDisplay()

            // Prikaži procenjeni domet u posebnom TextView-u
            if (refuel.estimatedRange > 0) {
                binding.tvEstimatedRange.text = String.format(
                    binding.root.context.getString(com.example.fuel_tracker_app.R.string.range_format),
                    refuel.estimatedRange.toInt()
                )
                binding.tvEstimatedRange.visibility = View.VISIBLE
            } else {
                binding.tvEstimatedRange.visibility = View.GONE
            }

            // Prikaži datum
            val date = refuel.date.toDate()
            binding.tvDate.text = dateFormat.format(date)
            binding.tvTime.text = timeFormat.format(date)

            // Promeni boju ikone na osnovu troškova
            if (refuel.totalCost > 100) {
                // Crvena za skupe refuels (preko 100 KM)
                binding.iconBackground.setBackgroundResource(com.example.fuel_tracker_app.R.drawable.circle_background_red)
            } else if (refuel.totalCost > 50) {
                // Narandžasta za srednje refuels (50-100 KM)
                binding.iconBackground.setBackgroundResource(com.example.fuel_tracker_app.R.drawable.circle_background_orange)
            } else {
                // Zelena za jeftine refuels (ispod 50 KM)
                binding.iconBackground.setBackgroundResource(com.example.fuel_tracker_app.R.drawable.circle_background_green)
            }
        }
    }
}

class RefuelDiffCallback : DiffUtil.ItemCallback<Refuel>() {
    override fun areItemsTheSame(oldItem: Refuel, newItem: Refuel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Refuel, newItem: Refuel): Boolean {
        return oldItem == newItem
    }
}