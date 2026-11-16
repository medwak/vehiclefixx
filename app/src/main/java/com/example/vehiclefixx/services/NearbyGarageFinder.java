package com.example.vehiclefixx.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.vehiclefixx.BuildConfig;
import com.example.vehiclefixx.data.Garage;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Finds real garages around a point with the Places SDK for Android using
 * Places API (New) Nearby Search, ranked by distance.
 *
 * <p>This replaces the retired {@code com.google.android.gms.location.places} API.
 * The API key must have "Places API (New)" enabled.
 */
public final class NearbyGarageFinder {
    private static final String TAG = "NearbyGarageFinder";

    /** Google place types that represent places that fix vehicles. */
    private static final List<String> GARAGE_TYPES = Arrays.asList("car_repair");

    /** Only request the fields we show: Places bills by field mask. */
    private static final List<Place.Field> FIELDS = Arrays.asList(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING,
            Place.Field.USER_RATING_COUNT,
            Place.Field.NATIONAL_PHONE_NUMBER,
            Place.Field.CURRENT_OPENING_HOURS,
            Place.Field.UTC_OFFSET);

    /** Places Nearby Search returns at most 20 results per request. */
    private static final int MAX_RESULTS = 20;

    public interface Callback {
        void onGaragesFound(@NonNull List<Garage> garages);

        void onError(@NonNull Exception e);
    }

    private final PlacesClient placesClient;

    public NearbyGarageFinder(@NonNull Context context) {
        initPlaces(context);
        placesClient = Places.createClient(context.getApplicationContext());
    }

    /** Initialises the Places SDK once per process, with the new Places API enabled. */
    public static void initPlaces(@NonNull Context context) {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(),
                    BuildConfig.MAPS_API_KEY);
        }
    }

    /**
     * Searches for garages within {@code radiusMeters} (max 50 000) of {@code center},
     * nearest first.
     */
    public void findGarages(@NonNull LatLng center, double radiusMeters,
                            @NonNull Callback callback) {
        SearchNearbyRequest request = SearchNearbyRequest
                .builder(CircularBounds.newInstance(center, Math.min(radiusMeters, 50_000)), FIELDS)
                .setIncludedTypes(GARAGE_TYPES)
                .setMaxResultCount(MAX_RESULTS)
                .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                .build();

        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {
                    List<Garage> garages = new ArrayList<>();
                    for (Place place : response.getPlaces()) {
                        if (place.getId() == null || place.getLocation() == null) continue;
                        garages.add(toGarage(place, center));
                    }
                    // Already distance-ranked by the API; sort defensively by straight-line distance.
                    Collections.sort(garages,
                            (a, b) -> Float.compare(a.getDistanceMeters(), b.getDistanceMeters()));
                    callback.onGaragesFound(garages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "searchNearby failed", e);
                    callback.onError(e);
                });
    }

    @NonNull
    private static Garage toGarage(@NonNull Place place, @NonNull LatLng from) {
        LatLng loc = place.getLocation();
        Boolean openNow = null;
        try {
            openNow = place.isOpen();
        } catch (Exception ignored) {
            // isOpen() needs opening hours + UTC offset; leave unknown if unavailable.
        }
        String name = place.getDisplayName() != null ? place.getDisplayName() : "Garage";
        return new Garage(place.getId(), name, place.getFormattedAddress(),
                place.getNationalPhoneNumber(), place.getRating(), place.getUserRatingCount(),
                openNow, loc, distanceBetween(from, loc));
    }

    public static float distanceBetween(@NonNull LatLng a, @NonNull LatLng b) {
        float[] results = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results);
        return results[0];
    }
}
