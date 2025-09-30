package com.example.fuel_tracker_app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fuel_tracker_app.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupNavigation()
        checkUserAuthentication()
    }

    // Podesi navigaciju
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Podesi bottom navigation sa nav controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Slušaj promjeene destinacije da bi pokazao/sakrio bottom navigation
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment -> {
                    // Sakrij bottom navigation na login/register ekranima
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    // Prikaži bottom navigation na ostalim ekranima
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }

    // Provjeeri je li korisnik prijavljen
    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (currentUser == null) {
            // Korisnik nije prijavljen, navigiraj na login
            navController.navigate(R.id.loginFragment)
        } else {
            // Korisnik je prijavljen, navigiraj na home
            navController.navigate(R.id.homeFragment)
        }
    }

    override fun onStart() {
        super.onStart()

        // Provjeeri ponovo kada se aktivnost pokrene
        // (korisno ako se korisnik odjavi iz druge aktivnosti)
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            // Ako smo na nekom od glavnih ekrana, vrati na login
            when (navController.currentDestination?.id) {
                R.id.homeFragment,
                R.id.vehiclesFragment,
                R.id.settingsFragment -> {
                    navController.navigate(R.id.loginFragment)
                }
            }
        }
    }
}