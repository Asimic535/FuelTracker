package com.example.fuel_tracker_app.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fuel_tracker_app.R
import com.example.fuel_tracker_app.databinding.FragmentSettingsBinding
import com.example.fuel_tracker_app.ui.vehicles.AddRefuelDialog
import com.example.fuel_tracker_app.ui.viewmodel.SettingsViewModel

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    // Podesi click listener-e
    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    // Prikaži dialog za potvrdu odjavljivanja
    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.logout_confirmation_message))
            .setPositiveButton(getString(R.string.sign_out)) { _, _ ->
                logout()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // Odjavi korisnika
    private fun logout() {
        // Obriši sve sačuvane podatke iz SharedPreferences
        AddRefuelDialog.clearSavedData(requireContext())

        viewModel.logout()
        findNavController().navigate(R.id.action_settings_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}