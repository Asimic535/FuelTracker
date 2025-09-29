# FuelTracker

Android aplikacija za praćenje tankovanja vozila napisana u Kotlin-u sa XML layout-ima.

## Opis

FuelTracker omogućava korisnicima da:
- Dodaju i upravljaju svojim vozilima (marka, model, godina, snaga, tip goriva, potrošnja)
- Prate tankovanja unošenjem ukupne cene i cene po litru (aplikacija automatski računa litre)
- Pregledaju procenjeni domet vozila na osnovu potrošnje i tankovanja
- Filtriraju podatke po vozilu i vremenskom periodu (mesec, godina, sve vreme)
- Koriste kalkulatore za potrebno gorivo i domet vozila
- Pamte poslednju cenu goriva za brže unose
- Pregledaju statistike ukupne potrošnje i troškova

## Tehnologija

- **Programski jezik**: Kotlin
- **UI**: XML layouts (bez Compose)
- **Arhitektura**: MVVM (ViewModel + Repository)
- **Asinhroni kod**: Kotlin Coroutines + Flow
- **Navigacija**: Navigation Component + BottomNavigationView
- **Backend**: Firebase Authentication + Cloud Firestore
- **Min SDK**: 24 (Android 7.0)

## Pokretanje aplikacije

### Korak 1: Kreiranje Firebase projekta

1. Idite na [Firebase Console](https://console.firebase.google.com/)
2. Kliknite "Create a project" ili "Add project"
3. Unesite naziv projekta (npr. "FuelTracker")
4. Pratite korake čarobnjaka za kreiranje projekta

### Korak 2: Konfiguracija Firebase Authentication

1. U Firebase konzoli idite na Authentication > Sign-in method
2. Omogućite "Email/Password" provider
3. Kliknite "Save"

### Korak 3: Konfiguracija Cloud Firestore

1. U Firebase konzoli idite na Firestore Database
2. Kliknite "Create database"
3. Izaberite "Start in test mode" (privremeno za razvoj)
4. Izaberite regiju najbližu vama
5. Kliknite "Done"

### Korak 4: Dodavanje Android aplikacije

1. U Firebase konzoli kliknite na ikonu Android-a
2. Unesite package name: `com.example.fuel_tracker_app`
3. Unesite App nickname (opciono): "FuelTracker"
4. Unesite SHA-1 (opciono, možete dodati kasnije)
5. Kliknite "Register app"

### Korak 5: Preuzimanje google-services.json

1. Preuzmite `google-services.json` fajl
2. Postavite ga u `app/` direktorijum vašeg Android projekta

### Korak 6: Pokretanje aplikacije

1. Otvorite projekat u Android Studio
2. Syncujte gradle fajlove
3. Pokrenite aplikaciju na emulatoru ili fizičkom uređaju

### Korak 7: SHA-1 konfiguracija (opciono)

Ako bude potrebno dodavanje SHA-1 fingerprint-a:

```bash
./gradlew signingReport
```

Kopirajte SHA-1 i SHA-256 vrednosti iz debug keystore-a i dodajte ih u Firebase konzoli.

## Struktura Firestore baze

Aplikacija koristi sledeću strukturu dokumenata u Firestore:

```
users/{uid}
  ├── name: string (ime korisnika)
  └── email: string (email korisnika)

vehicles/{vehicleId}
  ├── make: string (marka vozila)
  ├── model: string (model vozila)
  ├── year: number (godina proizvodnje)
  ├── powerKw: number (snaga u kW)
  ├── fuelType: string (tip goriva)
  ├── fuelConsumption: number (potrošnja L/100km)
  ├── ownerId: string (ID vlasnika)
  ├── ownerName: string (ime vlasnika)
  ├── isPublic: boolean (javno vidljivo vozilo)
  └── createdAt: timestamp

refuels/{refuelId}
  ├── vehicleId: string (referenca na vozilo)
  ├── amount: number (količina u litrima)
  ├── pricePerLiter: number (cena po litru)
  ├── date: timestamp (datum tankovanja)
  ├── estimatedRange: number (procenjeni domet)
  ├── vehicleMake: string (marka vozila)
  ├── vehicleModel: string (model vozila)
  ├── ownerId: string (ID vlasnika)
  ├── ownerName: string (ime vlasnika)
  └── createdAt: timestamp
```

## Bezbednost

- `google-services.json` fajl je dodat u `.gitignore` i neće biti commitovan
- Firebase Firestore rules treba konfigurirati u Firebase konzoli za produkciju
- Testne rules dozvoljavaju čitanje/pisanje samo autentifikovanim korisnicima