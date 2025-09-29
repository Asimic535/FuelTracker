# FuelTracker

Android aplikacija za praćenje potrošnje goriva napisana u Kotlin-u sa XML layout-ima.

## Opis

FuelTracker omogućava korisnicima da:
- Dodaju i upravljaju svojim vozilima (marka, model, godina, snaga, tip goriva, potrošnja)
- Prate točenje unošenjem ukupne cijene i cijene po litru (aplikacija automatski računa litre)
- Pregledaju procijenjeni domet vozila na osnovu potrošnje i točenja
- Filtriraju podatke po vozilu i vremenskom razdoblju (mjesec, godina, svo vrijeme)
- Koriste kalkulatore za potrebno gorivo i domet vozila
- Pamte poslijednju cijenu goriva za brže unose
- Pregledaju statistike ukupne potrošnje i troškova

## Tehnologija

- **Programski jezik**: Kotlin
- **UI**: XML layouts (bez Compose)
- **Arhitektura**: MVVM (ViewModel + Repository)
- **Asinhroni kod**: Kotlin Coroutines + Flow
- **Navigacija**: Navigation Component + BottomNavigationView
- **Backend**: Firebase Authentication + Cloud Firestore
- **Min SDK**: 24 (Android 7.0)

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
  ├── pricePerLiter: number (cijena po litru)
  ├── date: timestamp (datum točenja)
  ├── estimatedRange: number (procijenjeni domet)
  ├── vehicleMake: string (marka vozila)
  ├── vehicleModel: string (model vozila)
  ├── ownerId: string (ID vlasnika)
  ├── ownerName: string (ime vlasnika)
  └── createdAt: timestamp
```

## Sigurnost

- `google-services.json` fajl je dodat u `.gitignore` i neće biti commitovan
- Firebase Firestore rules treba konfigurirati u Firebase konzoli za produkciju
- Testni rules dozvoljavaju čitanje/pisanje samo autentificiranim korisnicima
