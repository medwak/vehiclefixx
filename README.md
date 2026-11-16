# vehiclefixx
Android application to find the nearest garages and mechanics using Google Maps and GPS.

## How "find a garage near me" works

| Step | API |
| --- | --- |
| Get the driver's position | Fused Location Provider `getCurrentLocation()` (high accuracy, falls back to the last fix). Precise **or** approximate location permission is accepted. |
| Find real garages nearby | **Places API (New) Nearby Search** for type `car_repair`, ranked by distance, 10 km radius. Shows name, address, rating, open-now and distance. |
| Search near another place | Places SDK `AutocompleteSupportFragment`: pick any address and the garage search moves there. |
| Act on a garage | Long-press the info window to get **Directions** in Google Maps or **Call** the garage. |
| Find online mechanics | GeoFire radius query on `employees_available`, live updates as mechanics come online, move or go offline. |

## Setup

1. In Google Cloud Console enable **Maps SDK for Android** and **Places API (New)** for your key.
2. Add the key to `local.properties` (not committed):

   ```
   MAPS_API_KEY=AIza...
   ```

3. Open in Android Studio (AGP 8.7, JDK 17, compileSdk 35) and run.
