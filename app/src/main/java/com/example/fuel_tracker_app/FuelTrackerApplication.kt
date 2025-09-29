package com.example.fuel_tracker_app

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class FuelTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Omogući Firestore offline persistence za rad bez interneta
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings

            android.util.Log.d("FuelTrackerApp", "Firestore offline persistence enabled")
        } catch (e: Exception) {
            android.util.Log.e("FuelTrackerApp", "Error enabling Firestore persistence: ${e.message}")
        }
    }
}