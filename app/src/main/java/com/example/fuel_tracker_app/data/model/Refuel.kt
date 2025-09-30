package com.example.fuel_tracker_app.data.model

import com.google.firebase.Timestamp
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Model klasa za točenje sa informacijama o vlasniku
@Parcelize
data class Refuel(
    val id: String = "", // ID dokumenta u Firestore bazi
    val vehicleId: String = "", // ID vozila kome pripada ovo tankovanje
    val amount: Double = 0.0, // Količina goriva u litrima
    val pricePerLiter: Double = 0.0, // Cijena po litru u KM
    val totalCost: Double = 0.0, //  Ukupna cijena (automatski izračunato)
    val estimatedRange: Double = 0.0, //  Procenjeni doseg u km (automatski izračunato)
    val date: Timestamp = Timestamp.now(), // Datum točenja
    val ownerId: String = "", // ID korisnika koji je dodao ovo točenje
    val ownerName: String = "", // Ime korisnika koji je dodao ovo točenje
    val vehicleMake: String = "", // Keširani proizvođač vozila za prikaz
    val vehicleModel: String = "", // Keširani model vozila za prikaz
    val vehicleFuelConsumption: Double = 0.0, // Keširano za kalkulaciju dosega
    val createdAt: Timestamp = Timestamp.now() // Vrijeme kreiranja zapisa
) : Parcelable {
    // Pomoćna funkcija za provjeru je li trenutni korisnik vlasnik
    fun isOwnedBy(userId: String): Boolean = ownerId == userId

    // Pomoćna funkcija za dobijanje prikaza vozila
    fun getVehicleDisplay(): String = if (vehicleMake.isNotEmpty() && vehicleModel.isNotEmpty()) {
        "$vehicleMake $vehicleModel"
    } else {
        "Unknown Vehicle"
    }

    // Pomoćna funkcija za dobijanje teksta vlasnika za prikaz
    fun getOwnerDisplay(): String = if (ownerName.isNotEmpty()) "by $ownerName" else "by Unknown"

    // Pomoćna funkcija za formatiran prikaz cijene
    fun getFormattedTotalCost(): String = String.format("%.2f KM", totalCost)

    // Pomoćna funkcija za formatiran prikaz cijene po litru
    fun getFormattedPricePerLiter(): String = String.format("%.2f KM/L", pricePerLiter)

    // Pomoćna funkcija za formatiran prikaz dosega
    fun getFormattedRange(): String = if (estimatedRange > 0) {
        String.format("~%.0f km", estimatedRange)
    } else {
        "N/A"
    }

    // Funkcija za izračunavanje ukupne cijene
    companion object {
        fun calculateTotalCost(amount: Double, pricePerLiter: Double): Double {
            return amount * pricePerLiter
        }

        // Funkcija za izračunavanje procijenjenog dosega
        fun calculateEstimatedRange(amount: Double, fuelConsumption: Double): Double {
            return if (fuelConsumption > 0) {
                (amount / fuelConsumption) * 100.0
            } else {
                0.0
            }
        }
    }
}