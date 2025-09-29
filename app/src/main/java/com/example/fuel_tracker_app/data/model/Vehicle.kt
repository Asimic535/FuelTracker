package com.example.fuel_tracker_app.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

// Model klasa za vozilo sa informacijama o vlasniku i podešavanjima vidljivosti
@Parcelize
data class Vehicle(
    val id: String = "", // ID dokumenta u Firestore bazi
    val make: String = "", // Proizvođač vozila (npr. "Volkswagen")
    val model: String = "", // Model vozila (npr. "Golf")
    val year: Int = 0, // Godina proizvodnje
    val powerKw: Double = 0.0, // Snaga motora u kW
    val fuelType: String = "", // Tip goriva (benzin, dizel, hibrid, itd.)
    val fuelConsumption: Double = 0.0, // Prosečna potrošnja goriva u L/100km
    val ownerId: String = "", // ID korisnika koji je vlasnik vozila
    val ownerName: String = "", // Ime vlasnika vozila za prikaz
    val isPublic: Boolean = true, // Da li je ovo vozilo vidljivo drugim korisnicima
    val createdAt: Timestamp = Timestamp.now() // Vreme kreiranja zapisa
) : Parcelable {
    // Pomoćna funkcija za proveru da li je trenutni korisnik vlasnik
    fun isOwnedBy(userId: String): Boolean = ownerId == userId

    // Pomoćna funkcija za dobijanje naslova za prikaz
    fun getDisplayTitle(): String = "$make $model ($year)"

    // Pomoćna funkcija za dobijanje teksta vlasnika za prikaz
    fun getOwnerDisplay(): String = if (ownerName.isNotEmpty()) "by $ownerName" else "by Unknown"

    // Funkcija za izračun potrebne količine goriva za određenu kilometražu
    fun calculateFuelNeeded(kilometers: Double): Double {
        if (fuelConsumption <= 0) return 0.0
        return (fuelConsumption * kilometers) / 100.0
    }

    // Funkcija za izračun procenjenog dometa na osnovu količine goriva
    fun calculateEstimatedRange(fuelAmount: Double): Double {
        if (fuelConsumption <= 0) return 0.0
        return (fuelAmount * 100.0) / fuelConsumption
    }

    // Funkcija za dobijanje teksta o potrošnji
    fun getConsumptionDisplay(): String = if (fuelConsumption > 0) {
        "${String.format("%.1f", fuelConsumption)} L/100km"
    } else {
        "Consumption not defined"
    }

    // Konverzija kW u konjske snage (KS)
    fun getPowerInHorsepower(): Int = (powerKw * 1.36).toInt()

    // Prikaz snage sa oba mernim jedinicama
    fun getPowerDisplay(): String = "$powerKw kW (${getPowerInHorsepower()} KS)"
}