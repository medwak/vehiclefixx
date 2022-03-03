package com.example.vehiclefixx.settings;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.vehiclefixx.R;
import com.example.vehiclefixx.data.mechanic;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private static final String TAG =
            "TAG :: " + CustomInfoWindowAdapter.class.getSimpleName() + " :: ";
    private final Activity activity;
    private TextView phone;
    private TextView skill;
    private RatingBar ratingBar;

    public CustomInfoWindowAdapter(Activity activity) {
        Log.d(TAG, "getInfoWindow");
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
        Gson gson = new Gson();
        View v;
        mechanic mech = gson.fromJson(marker.getSnippet(), mechanic.class);

        if (mech != null) {
            v = activity.getLayoutInflater().inflate(R.layout.mechanic_map_custom_info, null);
            TextView name = v.findViewById(R.id.name);
            TextView garagename = v.findViewById(R.id.garagename);
            phone = v.findViewById(R.id.phone);
            skill = v.findViewById(R.id.skill);
            TextView service = v.findViewById(R.id.service);
            ratingBar = v.findViewById(R.id.rating_bar);
            garagename.setText("city Garage");
            name.setText(mech.getName());
            skill.setText(mech.getSkill());
            service.setText(mech.getService());
            phone.setText(mech.getPhone());
            String rating = mech.getRatings();
            if (rating == null) rating = "0";
            ratingBar.setRating(Float.parseFloat(rating));
            Log.d(TAG, "getInfoContents :getRating: " +ratingBar.getRating());
        } else {
//            Employer employer = gson.fromJson(marker.getSnippet(), Employer.class);
//            v = activity.getLayoutInflater().inflate(R.layout.employer_map_custom_info, null);
//            name = v.findViewById(R.id.name);
//            phone = v.findViewById(R.id.phone);
//            ratingBar = v.findViewById(R.id.rating_bar);
//            name.setText(employer.getName());
//            phone.setText(employer.getPhone());
//            String rating = employer.getRatings();
//            if (rating == null) rating = "0";
//            ratingBar.setRating(Float.parseFloat(rating));
            return null;
        }

        return v;
    }
}
