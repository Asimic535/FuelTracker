package com.example.fuel_tracker_app.data.repo

import com.example.fuel_tracker_app.data.model.Refuel
import com.example.fuel_tracker_app.data.model.UserProfile
import com.example.fuel_tracker_app.data.model.Vehicle
import com.example.fuel_tracker_app.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Pretvara Firebase greške u korisno prijazne poruke
     */
    private fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "No internet connection. Using offline data."
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Access denied. Please check your permissions."
                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        "Database not found. Please set up Firestore database."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "Please sign in to continue."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Connection timeout. Please try again."
                    else -> "Connection error: ${exception.message}"
                }
            }
            else -> exception.message ?: "Unknown error occurred"
        }
    }

    // Prijava korisnika sa email i lozinkom
    suspend fun authSignIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.Success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.Error(Exception(getErrorMessage(e)))
        }
    }

    // Registracija novog korisnika
    suspend fun authSignUp(name: String, email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.Error(Exception("UID is null"))

            // Sačuvaj profil korisnika u Firestore
            val userProfile = UserProfile(name = name)
            firestore.collection("users")
                .document(uid)
                .set(userProfile)
                .await()

            Result.Success(uid)
        } catch (e: Exception) {
            Result.Error(Exception(getErrorMessage(e)))
        }
    }

    // Odjava korisnika
    fun authSignOut() {
        auth.signOut()
    }

    // Dobij ID trenutno prijavljenog korisnika
    fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Pretplaćivanje na sva javna vozila i privatna vozila trenutnog korisnika
    fun streamAllVehicles(currentUserId: String): Flow<List<Vehicle>> = callbackFlow {
        val listener = firestore.collection("vehicles")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(Exception(getErrorMessage(error)))
                    return@addSnapshotListener
                }

                val vehicles = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Vehicle::class.java)?.copy(id = doc.id)
                }?.filter { vehicle ->
                    // Prikaži javna vozila ili vozila u vlasništvu trenutnog korisnika
                    vehicle.isPublic || vehicle.ownerId == currentUserId
                } ?: emptyList()

                trySend(vehicles)
            }

        awaitClose { listener.remove() }
    }

    // Pretplaćivanje samo na vozila trenutnog korisnika (javna i privatna)
    fun streamMyVehicles(uid: String): Flow<List<Vehicle>> = callbackFlow {
        val listener = firestore.collection("vehicles")
            .whereEqualTo("ownerId", uid)
//            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val vehicles = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Vehicle::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(vehicles)
            }

        awaitClose { listener.remove() }
    }

    // Dodavanje novog vozila u globalnu kolekciju sa informacijama o vlasniku - SA VALIDACIJOM
    suspend fun addVehicle(vehicle: Vehicle): Result<String> {
        return try {
            // Validacija ulaznih podataka
            if (vehicle.ownerId.isBlank()) {
                return Result.Error(Exception("Owner ID is required"))
            }

            if (vehicle.make.isBlank() || vehicle.model.isBlank()) {
                return Result.Error(Exception("Vehicle make and model are required"))
            }

            if (vehicle.year < 1900 || vehicle.year > 2030) {
                return Result.Error(Exception("Invalid vehicle year"))
            }

            if (vehicle.powerKw < 0 || vehicle.powerKw > 2000) {
                return Result.Error(Exception("Invalid engine power"))
            }

            val docRef = firestore.collection("vehicles")
                .add(vehicle)
                .await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(Exception(getErrorMessage(e)))
        }
    }

    // Ažuriranje postojećeg vozila (samo vlasnik može ažurirati) - SA VALIDACIJOM
    suspend fun updateVehicle(vehicle: Vehicle, currentUserId: String): Result<Unit> {
        return try {

            // Validacija ulaznih podataka
            if (vehicle.id.isBlank() || currentUserId.isBlank()) {
                return Result.Error(Exception("Invalid vehicle ID or user ID"))
            }

            if (vehicle.make.isBlank() || vehicle.model.isBlank()) {
                return Result.Error(Exception("Vehicle make and model are required"))
            }

            // Koristi transakciju za osiguravanje atomičnosti i trenutnog vlasništva
            firestore.runTransaction { transaction ->
                val vehicleRef = firestore.collection("vehicles").document(vehicle.id)
                val currentDoc = transaction.get(vehicleRef)

                if (!currentDoc.exists()) {
                    throw Exception("Vehicle not found in database")
                }

                val currentVehicle = currentDoc.toObject(Vehicle::class.java)

                if (currentVehicle?.ownerId != currentUserId) {
                    throw Exception("You can only update your own vehicles")
                }

                // Ažuriranje sa validacijom trenutnog korisnika
                transaction.set(vehicleRef, vehicle)
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Brisanje vozila i svih njegovih točenja (samo vlasnik može brisati)
    suspend fun deleteVehicle(vehicleId: String, currentUserId: String): Result<Unit> {
        return try {

            // Validacija ulaznih podataka
            if (vehicleId.isBlank() || currentUserId.isBlank()) {
                return Result.Error(Exception("Invalid vehicle ID or user ID"))
            }

            // Prvo dobij sva točenja za ovo vozilo (van transakcije)
            val refuelsSnapshot = firestore.collection("refuels")
                .whereEqualTo("vehicleId", vehicleId)
                .get()
                .await()


            // Koristi transakciju za atomsku operaciju
            firestore.runTransaction { transaction ->
                // Prvo proveri da li korisnik poseduje vozilo unutar transakcije
                val vehicleRef = firestore.collection("vehicles").document(vehicleId)
                val vehicleDoc = transaction.get(vehicleRef)

                if (!vehicleDoc.exists()) {
                    throw Exception("Vehicle not found in database")
                }

                val vehicle = vehicleDoc.toObject(Vehicle::class.java)

                if (vehicle?.ownerId != currentUserId) {
                    throw Exception("You can only delete your own vehicles")
                }

                // Obriši sva točenja prvo
                refuelsSnapshot.documents.forEach { refuelDoc ->
                    transaction.delete(refuelDoc.reference)
                }

                // Zatim obriši vozilo
                transaction.delete(vehicleRef)
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Dodavanje novog točenja u globalnu kolekciju sa informacijama o vlasniku - SA VALIDACIJOM
    suspend fun addRefuel(refuel: Refuel): Result<String> {
        return try {
            // Validacija ulaznih podataka
            if (refuel.vehicleId.isBlank() || refuel.ownerId.isBlank()) {
                return Result.Error(Exception("Vehicle ID and owner ID are required"))
            }

            if (refuel.amount <= 0 || refuel.amount > 1000) {
                return Result.Error(Exception("Invalid fuel amount (must be between 0-1000 liters)"))
            }

            // Proveri da vozilo postoji i da korisnik ima pristup
            val vehicleDoc = firestore.collection("vehicles")
                .document(refuel.vehicleId)
                .get()
                .await()

            if (!vehicleDoc.exists()) {
                return Result.Error(Exception("Vehicle not found"))
            }

            val vehicle = vehicleDoc.toObject(Vehicle::class.java)
            if (vehicle?.ownerId != refuel.ownerId) {
                return Result.Error(Exception("You can only add refuels to your own vehicles"))
            }

            val docRef = firestore.collection("refuels")
                .add(refuel)
                .await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(Exception(getErrorMessage(e)))
        }
    }

    // Pretplaćivanje na točenja sa filtriranjem, prikazuje točenja svih korisničkih vozila
    fun streamRefuelsFiltered(
        currentUserId: String,
        vehicleIdOrNull: String?,
        fromDateOrNull: Timestamp?,
        toDateOrNull: Timestamp?,
        showOnlyMyRefuels: Boolean = false
    ): Flow<List<Refuel>> = callbackFlow {
        // Jednostavan upit koji ne zahtjeeva složene indekse
        val query: Query = if (showOnlyMyRefuels) {
            firestore.collection("refuels")
                .whereEqualTo("ownerId", currentUserId)
        } else {
            firestore.collection("refuels")
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(Exception(getErrorMessage(error)))
                return@addSnapshotListener
            }

            var refuels = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Refuel::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            // Filtriranje na klijentskoj strani za vozilo i datume
            vehicleIdOrNull?.let { vehicleId ->
                refuels = refuels.filter { it.vehicleId == vehicleId }
            }

            fromDateOrNull?.let { fromDate ->
                refuels = refuels.filter { it.date >= fromDate }
            }

            toDateOrNull?.let { toDate ->
                refuels = refuels.filter { it.date <= toDate }
            }

            // Sortiranje na klijentskoj strani po datumu (najnoviji prvo)
            refuels = refuels.sortedByDescending { it.date }

            trySend(refuels)
        }

        awaitClose { listener.remove() }
    }

    // Dobijanje profila korisnika
    suspend fun getUserProfile(uid: String): Result<UserProfile> {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val userProfile = document.toObject(UserProfile::class.java)
                ?: UserProfile()

            Result.Success(userProfile)
        } catch (e: Exception) {
            Result.Error(Exception(getErrorMessage(e)))
        }
    }

    // Dobijanje vozila po ID-ju (za proveru vlasništva)
    suspend fun getVehicleById(vehicleId: String): Result<Vehicle?> {
        return try {
            val document = firestore.collection("vehicles")
                .document(vehicleId)
                .get()
                .await()


            val vehicle = document.toObject(Vehicle::class.java)?.copy(id = document.id)
            Result.Success(vehicle)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}