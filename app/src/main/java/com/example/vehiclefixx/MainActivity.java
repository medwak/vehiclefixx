package com.example.vehiclefixx;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vehiclefixx.garages.garage_login;
import com.example.vehiclefixx.settings.vehicleFixx_Settings;
import com.example.vehiclefixx.users.drivers.driverLoginActivity;
import com.example.vehiclefixx.users.mechanics.mechanicLoginActivity;

    public class MainActivity extends AppCompatActivity {
        private static final String TAG = "TAG :: " + MainActivity.class.getSimpleName() + " :: ";
        private Button mechanic_btn, driver_btn,garage_btn;
        private Boolean connected, allowed,location;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            vehicleFixx_Settings settings =
                    new vehicleFixx_Settings(this);
            Log.d(TAG, "onCreate");
            location = settings.isLocationEnabled(this);
            settings.checkLocationPermission(this);
            settings.checkMap(this);
            connected = settings.checkInternet(this);
            allowed = settings.checkPermissionAllowed(this.getApplicationContext());

            mechanic_btn = findViewById(R.id.mechanic_btn);
            driver_btn = findViewById(R.id.driver_btn);
            garage_btn = findViewById(R.id.garage_btn);
            if(!location){
                vehicleFixx_Settings.showAlert(this, "Error!!!","Turn On Location");
            }
//        if(!allowed){
//            HomeApp_Settings.showAlert(this, "Error!!!","No or Internet Connection");
//        }
            mechanic_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, mechanicLoginActivity.class));
                }
            });
            driver_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, driverLoginActivity.class));
                }
            });
            garage_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, garage_login.class));
                }
            });

        }


    }
