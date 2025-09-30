package com.example.fuel_tracker_app.ui.login

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
import com.example.fuel_tracker_app.databinding.FragmentLoginBinding
import com.example.fuel_tracker_app.ui.viewmodel.LoginViewModel
import com.example.fuel_tracker_app.util.Result
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    // Podesi click listener-e
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.tvRegisterPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    // Posmatraj promjene u ViewModel-u
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnLogin.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.loginState.collect { result ->
                when (result) {
                    is Result.Success -> {
                        viewModel.resetState()
                        findNavController().navigate(R.id.action_login_to_home)
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.login_error, result.exception.message),
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
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_required)
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            return false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_required)
            return false
        }

        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_min_length)
            return false
        }

        // Očisti greške
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}