package com.example.vehiclefixx.users.mechanics;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vehiclefixx.R;

public class mechanicWorkView extends AppCompatActivity {
    private static final String TAG =
            "TAG :: " + mechanicWorkView.class.getSimpleName() + " :: ";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");

        setContentView(R.layout.activity_mechanic_work_view);
    }
}