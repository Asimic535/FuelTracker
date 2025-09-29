package com.example.fuel_tracker_app.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fuel_tracker_app.R
import com.example.fuel_tracker_app.databinding.FragmentRegisterBinding
import com.example.fuel_tracker_app.ui.viewmodel.RegisterViewModel
import com.example.fuel_tracker_app.util.Result
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    // Podesi click listener-e
    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                viewModel.register(name, email, password)
            }
        }

        binding.tvLoginPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    // Posmatraj promene u ViewModel-u
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnRegister.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.registerState.collect { result ->
                when (result) {
                    is Result.Success -> {
                        viewModel.resetState()
                        Toast.makeText(
                            requireContext(),
                            "Uspešno ste se registrovali!",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.action_register_to_home)
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Greška pri registraciji: ${result.exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }

    // Validiraj unos korisnika
    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Ime je obavezno"
            isValid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email je obavezan"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Unesite validan email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Lozinka je obavezna"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Lozinka mora imati najmanje 6 karaktera"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Potvrdite lozinku"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Lozinke se ne poklapaju"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}