package com.example.vehiclefixx.settings;

import android.app.Activity;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.vehiclefixx.R;
import com.example.vehiclefixx.data.Garage;
import com.example.vehiclefixx.data.mechanic;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Info window for the driver map. The marker's tag decides the layout:
 * a {@link Garage} (from Google Places) or a registered {@link mechanic}.
 * Markers with no tag fall back to a JSON snippet, then to the default window.
 */
public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private final Activity activity;

    public CustomInfoWindowAdapter(Activity activity) {
        this.activity = activity;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null;
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof Garage) {
            return garageView((Garage) tag);
        }
        if (tag instanceof mechanic) {
            return mechanicView((mechanic) tag);
        }
        // Legacy markers (mechanic map) carry a JSON user in the snippet.
        String snippet = marker.getSnippet();
        if (snippet != null && snippet.trim().startsWith("{")) {
            try {
                mechanic mech = new Gson().fromJson(snippet, mechanic.class);
                if (mech != null) return mechanicView(mech);
            } catch (JsonSyntaxException ignored) {
                // not a user JSON; use the default window
            }
        }
        return null;
    }

    private View garageView(Garage garage) {
        View v = activity.getLayoutInflater().inflate(R.layout.garage_map_custom_info, null);
        ((TextView) v.findViewById(R.id.garage_name)).setText(garage.getName());

        TextView address = v.findViewById(R.id.garage_address);
        if (garage.getAddress() != null) {
            address.setText(garage.getAddress());
        } else {
            address.setVisibility(View.GONE);
        }

        RatingBar ratingBar = v.findViewById(R.id.garage_rating);
        TextView ratingCount = v.findViewById(R.id.garage_rating_count);
        if (garage.getRating() != null) {
            ratingBar.setRating(garage.getRating().floatValue());
            Integer count = garage.getRatingCount();
            ratingCount.setText(count != null
                    ? String.format(java.util.Locale.getDefault(), "%.1f (%d)", garage.getRating(), count)
                    : String.valueOf(garage.getRating()));
        } else {
            ratingBar.setVisibility(View.GONE);
            ratingCount.setVisibility(View.GONE);
        }

        StringBuilder meta = new StringBuilder(garage.getFormattedDistance());
        if (garage.getOpenNow() != null) {
            meta.append(" · ").append(activity.getString(
                    garage.getOpenNow() ? R.string.open_now : R.string.closed_now));
        }
        meta.append(" · ").append(activity.getString(R.string.long_press_options));
        ((TextView) v.findViewById(R.id.garage_meta)).setText(meta);
        return v;
    }

    private View mechanicView(mechanic mech) {
        View v = activity.getLayoutInflater().inflate(R.layout.mechanic_map_custom_info, null);
        TextView name = v.findViewById(R.id.name);
        TextView garagename = v.findViewById(R.id.garagename);
        TextView phone = v.findViewById(R.id.phone);
        TextView skill = v.findViewById(R.id.skill);
        TextView service = v.findViewById(R.id.service);
        RatingBar ratingBar = v.findViewById(R.id.rating_bar);
        garagename.setText(activity.getString(R.string.mechanic));
        name.setText(mech.getName());
        skill.setText(mech.getSkill());
        service.setText(mech.getService());
        phone.setText(mech.getPhone());
        float rating = 0f;
        try {
            if (mech.getRatings() != null) rating = Float.parseFloat(mech.getRatings());
        } catch (NumberFormatException ignored) {
            // keep 0
        }
        ratingBar.setRating(rating);
        return v;
    }
}
