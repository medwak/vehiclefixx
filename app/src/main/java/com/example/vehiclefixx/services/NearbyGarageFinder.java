package com.example.vehiclefixx.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.vehiclefixx.BuildConfig;
import com.example.vehiclefixx.data.Garage;
import com.example.vehiclefixx.data.VehicleType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Finds real garages around a point with the Places SDK for Android (Places API New),
 * ranked by distance, for the driver's {@link VehicleType}: Nearby Search by place type for
 * cars, Text Search for motorbike and lorry workshops.
 *
 * <p>This replaces the retired {@code com.google.android.gms.location.places} API.
 * The API key must have "Places API (New)" enabled.
 */
public final class NearbyGarageFinder {
    private static final String TAG = "NearbyGarageFinder";

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

    /** Places Nearby / Text Search return at most 20 results per request. */
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
     * Searches for garages that repair {@code vehicleType} within {@code radiusMeters}
     * (max 50 000) of {@code center}, nearest first.
     */
    public void findGarages(@NonNull LatLng center, double radiusMeters,
                            @NonNull VehicleType vehicleType, @NonNull Callback callback) {
        double radius = Math.min(radiusMeters, 50_000);
        if (vehicleType.getPlaceType() != null) {
            SearchNearbyRequest request = SearchNearbyRequest
                    .builder(CircularBounds.newInstance(center, radius), FIELDS)
                    .setIncludedTypes(Collections.singletonList(vehicleType.getPlaceType()))
                    .setMaxResultCount(MAX_RESULTS)
                    .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                    .build();
            placesClient.searchNearby(request)
                    .addOnSuccessListener(r -> deliver(r.getPlaces(), center, radius, callback))
                    .addOnFailureListener(e -> fail(e, callback));
        } else {
            // Text Search only accepts a rectangle as a hard restriction; results are
            // trimmed to the circle afterwards.
            SearchByTextRequest request = SearchByTextRequest
                    .builder(vehicleType.getTextQuery(), FIELDS)
                    .setLocationRestriction(boundsAround(center, radius))
                    .setMaxResultCount(MAX_RESULTS)
                    .setRankPreference(SearchByTextRequest.RankPreference.DISTANCE)
                    .build();
            placesClient.searchByText(request)
                    .addOnSuccessListener(r -> deliver(r.getPlaces(), center, radius, callback))
                    .addOnFailureListener(e -> fail(e, callback));
        }
    }

    private static void deliver(@NonNull List<Place> places, @NonNull LatLng center,
                                double radius, @NonNull Callback callback) {
        List<Garage> garages = new ArrayList<>();
        for (Place place : places) {
            if (place.getId() == null || place.getLocation() == null) continue;
            Garage garage = toGarage(place, center);
            if (garage.getDistanceMeters() <= radius) garages.add(garage);
        }
        Collections.sort(garages,
                (a, b) -> Float.compare(a.getDistanceMeters(), b.getDistanceMeters()));
        callback.onGaragesFound(garages);
    }

    private static void fail(@NonNull Exception e, @NonNull Callback callback) {
        Log.e(TAG, "Places garage search failed", e);
        callback.onError(e);
    }

    /** Square that encloses the circle of {@code radiusMeters} around {@code center}. */
    @NonNull
    private static RectangularBounds boundsAround(@NonNull LatLng center, double radiusMeters) {
        double latDelta = Math.toDegrees(radiusMeters / 6_371_000d);
        double lngDelta = latDelta / Math.max(Math.cos(Math.toRadians(center.latitude)), 0.01);
        return RectangularBounds.newInstance(
                new LatLng(Math.max(center.latitude - latDelta, -90), center.longitude - lngDelta),
                new LatLng(Math.min(center.latitude + latDelta, 90), center.longitude + lngDelta));
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
