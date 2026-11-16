package com.example.vehiclefixx.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

/**
 * A garage / car-repair business returned by Google Places Nearby Search.
 * Stored as a {@link com.google.android.gms.maps.model.Marker} tag on the driver map.
 */
public class Garage {
    private final String placeId;
    private final String name;
    @Nullable private final String address;
    @Nullable private final String phone;
    @Nullable private final Double rating;
    @Nullable private final Integer ratingCount;
    @Nullable private final Boolean openNow;
    private final LatLng location;
    private final float distanceMeters;

    public Garage(@NonNull String placeId, @NonNull String name, @Nullable String address,
                  @Nullable String phone, @Nullable Double rating, @Nullable Integer ratingCount,
                  @Nullable Boolean openNow, @NonNull LatLng location, float distanceMeters) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.openNow = openNow;
        this.location = location;
        this.distanceMeters = distanceMeters;
    }

    @NonNull public String getPlaceId() { return placeId; }
    @NonNull public String getName() { return name; }
    @Nullable public String getAddress() { return address; }
    @Nullable public String getPhone() { return phone; }
    @Nullable public Double getRating() { return rating; }
    @Nullable public Integer getRatingCount() { return ratingCount; }
    @Nullable public Boolean getOpenNow() { return openNow; }
    @NonNull public LatLng getLocation() { return location; }
    public float getDistanceMeters() { return distanceMeters; }

    /** "850 m" or "3.2 km". */
    @NonNull
    public String getFormattedDistance() {
        return formatDistance(distanceMeters);
    }

    @NonNull
    public static String formatDistance(float meters) {
        if (meters < 1000) {
            return Math.round(meters) + " m";
        }
        return String.format(java.util.Locale.getDefault(), "%.1f km", meters / 1000f);
    }
}
