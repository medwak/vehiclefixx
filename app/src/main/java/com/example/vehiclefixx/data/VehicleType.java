package com.example.vehiclefixx.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.vehiclefixx.R;

/**
 * The kind of vehicle the driver needs fixed. Each type maps to the Google Places search
 * that finds garages able to work on it.
 *
 * <p>Google has a dedicated place type for car garages ({@code car_repair}), so cars use
 * Nearby Search. Motorbike and lorry workshops have no dedicated type, so they use Text Search
 * with a query, ranked by distance.
 */
public enum VehicleType {
    CAR(R.string.vehicle_car, "car_repair", null),
    MOTORBIKE(R.string.vehicle_motorbike, null, "motorcycle repair garage"),
    LORRY(R.string.vehicle_lorry, null, "truck lorry repair garage");

    @StringRes private final int label;
    /** Places API (New) type for Nearby Search, or null to use Text Search. */
    @Nullable private final String placeType;
    /** Text Search query when there is no dedicated place type. */
    @Nullable private final String textQuery;

    VehicleType(@StringRes int label, @Nullable String placeType, @Nullable String textQuery) {
        this.label = label;
        this.placeType = placeType;
        this.textQuery = textQuery;
    }

    @StringRes
    public int getLabel() {
        return label;
    }

    @Nullable
    public String getPlaceType() {
        return placeType;
    }

    @Nullable
    public String getTextQuery() {
        return textQuery;
    }

    /** Parses a stored name, defaulting to {@link #CAR}. */
    @NonNull
    public static VehicleType fromName(@Nullable String name) {
        if (name != null) {
            for (VehicleType type : values()) {
                if (type.name().equals(name)) return type;
            }
        }
        return CAR;
    }
}
