package com.example.vehiclefixx.users.mechanics;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.vehiclefixx.MainActivity;
import com.example.vehiclefixx.R;
import com.example.vehiclefixx.data.driver;
import com.example.vehiclefixx.data.driverRequest;
import com.example.vehiclefixx.data.mechanic;
import com.example.vehiclefixx.services.DownloadTask;
import com.example.vehiclefixx.services.SendSmsDriver;
import com.example.vehiclefixx.settings.CustomInfoWindowAdapter;
import com.example.vehiclefixx.settings.vehicleFixx_Settings;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class mechanicMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "TAG :: " + mechanicMapActivity.class.getSimpleName() + " :: ";
    private static final int DEFAULT_ZOOM = 75;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static int AUTOCOMPLETE_REQUEST_CODE = 1;
    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private final int SET_PROFILE = 19234;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private CameraPosition cameraPosition;
    private driverRequest employerRequest;
    private boolean locationPermissionGranted;
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;
    private Location mLastLocation;
    private SwitchMaterial mWorkingSwitch;
    private TextView cardView;
    private boolean profile_good;
    private Marker employeeMarker, employerMarker;
    private HashSet<driverRequest> employerRequests;
    private ProgressDialog loader;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean notified;
    private final ValueEventListener employeeNotificationVE = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            employerRequests = new HashSet<>();
            int x = 0;
            for (DataSnapshot data : snapshot.getChildren()) {
                if (data.getKey() != null) {
                    Log.d(TAG, "employeeNotificationVE onDataChange getKey :: " + data.getKey());
                    Log.d(TAG,
                            "employeeNotificationVE onDataChange getValue :: " + data.getValue());
                    for (DataSnapshot child : data.getChildren()) {
                        if ((child != null) && (child.getKey() != null)) {
                            Log.d(TAG,
                                    "employeeNotificationVE child getKey :: " + child.getKey());
                            if (child.getKey().equals(currentUser.getUid())) {
                                x += 1;
                                Log.d(TAG,
                                        "employeeNotificationVE equals child.getKey().equals :: " + child.getKey());
                                Log.d(TAG, "employeeNotificationVE data getKey :: " + data.getKey());
                                Log.d(TAG,
                                        "employeeNotificationVE onDataChange getValue :: " + child.getValue());
                                employerRequest =
                                        data.child(currentUser.getUid()).getValue(driverRequest.class);
                                employerRequest.setEmployerId(data.getKey());
                                employerRequests.add(employerRequest);
                            }
                        }
                    }
                }
            }
            if (x > 0) {
                cardView.setTextColor(getResources().getColor(R.color.Chartreuse));
                notified = true;
            } else {
                Log.d(TAG, "notifyVE onDataChange snapshot.exists() :else: NOT ");
                cardView.setTextColor(getResources().getColor(R.color.black));
                notified=false;
            }
            cardView.setText(String.valueOf(x));

        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };
    private Button mLogout, mSettings, mRideStatus, mHistory;
    private SupportMapFragment mapFrag;
    private Marker mCurrLocationMarker;
    private vehicleFixx_Settings settings;
    private DatabaseReference available;
    private DatabaseReference working, employees_notified, work_in_progress, employers;
    private String employerID = "";
    private String current_user_phone;
    private GeoFire geoFireAvailable, geoFireWorking;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        setContentView(R.layout.activity_mechanic_map);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "onCreate " + currentUser.getUid());

        if (currentUser != null) {
            this.setTitle(currentUser.getDisplayName().toUpperCase());
        }


        loader = new ProgressDialog(this);

        mSettings = findViewById(R.id.settings);
        mLogout = findViewById(R.id.logout);
        mWorkingSwitch = findViewById(R.id.workingSwitch);
        cardView = findViewById(R.id.notification_id);
        available = FirebaseDatabase.getInstance().getReference("employees_available");
        working = FirebaseDatabase.getInstance().getReference("employees_working");
        work_in_progress = FirebaseDatabase.getInstance().getReference("work_in_progress");
        employees_notified = FirebaseDatabase.getInstance().getReference("employees_notified");

        employers = FirebaseDatabase
                .getInstance()
                .getReference("Users").child("Employer");
        FirebaseDatabase.getInstance().getReference("Users").child("Employee")
                .child(currentUser.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    mechanic mech =
                            task.getResult().getValue(mechanic.class);
                    current_user_phone = mech.getPhone();
                    profile_good = mech.getSkill() != null;
                    Log.d(TAG, "updateLocationUI onComplete " + mech.getSkill());
                }
            }
        });
        geoFireAvailable = new GeoFire(available);
        geoFireWorking = new GeoFire(working);
//        employees_notified.addValueEventListener(notifyVE);
        cardView.setEnabled(false);
        cardView.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(EmployeeMapActivity.this, EmployeeWorkView.class));
                Log.d(TAG, "onClick :: cardView.getText()" + cardView.getText());
                if (cardView.getText().toString().equals(String.valueOf(0))) {
                    Log.d(TAG, "cardView.getText()");
//                    employees_notified.addValueEventListener(notifyVE);
                } else
                    confirmRequest();
            }
        });
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(mechanicMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mSettings.setOnClickListener(v -> startActivityForResult(new Intent(mechanicMapActivity.this,
                mechanicSettingsActivity.class), SET_PROFILE));
        mWorkingSwitch.setEnabled(false);
        mWorkingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (notified) {
                    confirmRequest();
                } else {
                    if (profile_good) {
                        findWork();
                        mWorkingSwitch.setBackground(getResources().getDrawable(R.drawable.online_button));
                        mWorkingSwitch.setText(getResources().getText(R.string.online));
                    } else {
                        vehicleFixx_Settings.showAlert(mechanicMapActivity.this, "Profile Update"
                                , "Please update your profile before searching for work");
                        mWorkingSwitch.setChecked(false);
                    }

                }

            } else {
                cancelWork();
                mWorkingSwitch.setBackground(getResources().getDrawable(R.drawable.offline_button));
                mWorkingSwitch.setText(getResources().getText(R.string.offline));

            }
        });

    }

    private void confirmRequest() {
        Log.d(TAG, "confirmRequests  :: " + employerRequests.size());
        for (driverRequest requests : employerRequests) {
            Log.d(TAG, "confirmRequest :: " + requests.getEmployerId());

            employers.child(requests.getEmployerId())
                    .get()
                    .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "employerRequests " +
                                            "<DataSnapshot> " +
                                            "task. " +
                                            "isSuccessful");
                                    driver driv =
                                            task.getResult().getValue(driver.class);
                                    if (driv != null) {
                                        new AlertDialog.Builder(mechanicMapActivity.this)
                                                .setTitle("Accept work")
                                                .setMessage("Would you like to work for " + driv.getName() +
                                                        " ?")
                                                .setIcon(android.R.drawable.ic_dialog_info)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Log.d(TAG, "AlertDialog.setPositiveButton :: ");
                                                        acceptWork(driv);
                                                    }
                                                })
                                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Log.d(TAG, "AlertDialog.setNegativeButton :: ");
                                                        jobRejected(driv);
                                                    }
                                                }).show();
                                        Log.d(TAG,
                                                "employerRequests : DataSnapshot :: " + driv.getName());
                                    }


                                } else {
                                    Log.d(TAG, "employerRequests " +
                                            "  :task.notSuccessful(): ");
                                }
                            }
                    );
        }

    }

    private void rejectWork(driver driv) {
        notified = false;
        Log.d(TAG, "rejectWork:: ");
    }

    private void acceptWork(driver driv) {
        Log.d(TAG, "acceptWork :: ");
        if (employerRequest != null) {

            List<Double> map = (List<Double>) employerRequest.getL();
            double locationLat = 0;
            double locationLng = 0;
            if (map.get(0) != null) {
                locationLat = Double.parseDouble(map.get(0).toString());
            }
            if (map.get(1) != null) {
                locationLng = Double.parseDouble(map.get(1).toString());
            }
            LatLng employerLatLng = new LatLng(locationLat, locationLng);
//            jobAccepted(locationLat, locationLng);
            if (employerMarker != null) {
                employerMarker.remove();
            }
            loader.setTitle("Fetching Driver location");
            loader.setCanceledOnTouchOutside(false);
            loader.show();
            FirebaseDatabase
                    .getInstance()
                    .getReference("Users")
                    .child("Employer")
                    .child(employerRequest.getEmployerId())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (task.isSuccessful()) {
//                                loader.dismiss();
                                Log.d(TAG, "Task" +
                                        "<DataSnapshot> " +
                                        "task. " +
                                        "isSuccessful");
                                DataSnapshot dataSnapshot1 = task.getResult();
                                Log.d(TAG, "acceptWork dataSnapshot1" +
                                        " getKey :: " + dataSnapshot1.getKey());
                                Log.d(TAG, "acceptWorkq dataSnapshot1" +
                                        " getValue :: " + dataSnapshot1.getValue());
                                driver driv =
                                        task.getResult().getValue(driver.class);

                                if (driv != null) {
                                    Log.d(TAG,
                                            "acceptWork Mechanic :: " + driv.getName());
                                    Log.d(TAG,
                                            "acceptWork Mechanic :: " + driv.getPhone());
                                    Log.d(TAG,
                                            "acceptWork Mechanic :: " + driv.getEmail());
                                    Gson gson = new Gson();
                                    String markerInfoString =
                                            gson.toJson(driv);
                                    SendSmsDriver sendSms = new SendSmsDriver(mechanicMapActivity.this,
                                            currentUser.getDisplayName(),
                                            current_user_phone, work_in_progress
                                    );

                                    HashMap<String, Object> userInfo = new HashMap<>();
                                    userInfo.put("employee", currentUser.getUid());
                                    userInfo.put("employer", driv.getId());
                                    userInfo.put("completed", "0");
                                    work_in_progress
                                            .child(employerID)
                                            .updateChildren(userInfo)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Log.d(TAG, "onComplete :: isSuccessful");

                                                    } else {
                                                        if (task.getException() != null) {
                                                            Log.e(TAG, task.getException().getMessage(),
                                                                    task.getException());
                                                            loader.dismiss();
                                                        } else Log.e(TAG, "onComplete :: error");
                                                    }
                                                }
                                            });
                                    driver[] emps = {driv};
                                    try {
                                        String re = sendSms.execute(emps).get(2,
                                                TimeUnit.SECONDS);
                                        Log.d(TAG, "setOnInfoWindowLongClickListener :: " +
                                                "mSmsTask.execute: " + re);
                                    } catch (ExecutionException e) {
                                        Log.e(TAG,
                                                "setOnInfoWindowLongClickListener :ExecutionException: " + e.getMessage(), e.fillInStackTrace());
                                    } catch (InterruptedException e) {
                                        Log.e(TAG,
                                                "setOnInfoWindowLongClickListener :InterruptedException: " + e.getMessage(), e.fillInStackTrace());
                                    } catch (TimeoutException e) {
                                        Log.e(TAG,
                                                "setOnInfoWindowLongClickListener :TimeoutException: " + e.getMessage(), e.fillInStackTrace());
                                    }

                                    mMap.addMarker(
                                            new MarkerOptions()
                                                    .position(employerLatLng)
                                                    .title(driv.getName())
                                                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.jobicon))
                                                    .snippet(markerInfoString))
                                            .showInfoWindow();
                                    if (locationPermissionGranted) {
                                        if (ActivityCompat.checkSelfPermission(mechanicMapActivity.this,
                                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                                ActivityCompat.checkSelfPermission(mechanicMapActivity.this,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                                            // TODO: Consider calling
                                            //    ActivityCompat#requestPermissions
                                            // here to request the missing permissions, and then overriding
                                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            //                                          int[] grantResults)
                                            // to handle the case where the user grants the permission. See the documentation
                                            // for ActivityCompat#requestPermissions for more details.
                                            Log.d(TAG,
                                                    "acceptWork  locationPermissionGranted");
                                            return;
                                        }
                                        Task<Location> locationResult = mFusedLocationClient.getLastLocation();
                                        locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Location> task) {
                                                if (task.isSuccessful()) {
                                                    loader.dismiss();
                                                    Log.d(TAG, "acceptWork onComplete.isSuccessful :: ");
                                                    lastKnownLocation = task.getResult();
                                                    if (lastKnownLocation != null) {
                                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                                                new LatLng(lastKnownLocation.getLatitude(),
                                                                        lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                                        LatLng employeeLoc =
                                                                new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                                        String url =
                                                                vehicleFixx_Settings.getDirectionsUrl(employeeLoc, employerLatLng);
                                                        DownloadTask downloadTask =
                                                                new DownloadTask(mechanicMapActivity.this,
                                                                        mMap);
                                                        // Start downloading json data from Google Directions API
                                                        downloadTask.execute(url);
                                                        jobAccepted(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                                                    }
                                                } else {
                                                    loader.dismiss();
                                                    if (task.getException() != null) {
                                                        Log.d(TAG, "acceptWork onComplete" +
                                                                        ".isSuccessful :: " + task.getException().getMessage(),
                                                                task.getException().getCause());
                                                    } else {
                                                        Log.d(TAG, "acceptWork onComplete" +
                                                                ".isSuccessful :: EEEErrre");
                                                    }
                                                }
                                            }
                                        });

                                    }
                                }
                            } else {
                                loader.dismiss();
                                if (task.getException() != null) {
                                    Log.d(TAG,
                                            "acceptWork.getReference() :: NOT " +
                                                    "isSuccessful " + task.getException().getMessage());
                                    if (Objects.equals(task.getException().getMessage(), "Client is offline")) {
                                        vehicleFixx_Settings.showAlert(mechanicMapActivity.this, "Connection Error",
                                                "You are " +
                                                        "offline");
                                    }
                                } else Log.d(TAG, "acceptWork.getReference() :: " +
                                        "NOT isSuccessful");
                            }
                        }
                    });

        } else {
            Log.d(TAG, "acceptWork.() :: employerRequest == null");
//            employees_notified.addValueEventListener(notifyVE);
        }

    }


    private void jobAccepted(double locationLat, double locationLng) {

        geoFireWorking.setLocation(currentUser.getUid(),
                new GeoLocation(locationLat, locationLng),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            Log.d(TAG, error.getMessage(), error.toException());
                        } else {
                            Log.d(TAG, "geoFireWorking.setLocation :: " +
                                    key);
                            Log.d(TAG,
                                    "sucessfully setLocation " +
                                            "geoFireWorking :: " + currentUser.getUid());
                            Toast.makeText(mechanicMapActivity.this, "sucessfully removed " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        available.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    geoFireAvailable.removeLocation(currentUser.getUid(),
                            new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (error != null) {
                                        Log.d(TAG, error.getMessage(), error.toException());
                                    } else {
                                        Log.d(TAG, "geoFireAvailable.removeLocation :: " +
                                                key);
                                        Log.d(TAG,
                                                "sucessfully removeLocation " +
                                                        "geoFireAvailable :: " + currentUser.getUid());
                                        Toast.makeText(mechanicMapActivity.this, "sucessfully removed " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void jobRejected(driver driv){
        Log.d(TAG,"jobRejected");
        employees_notified.child(driv.getId()).child(currentUser.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG,
                        "sucessfully removeLocation " +
                                "geoFireAvailable :: " + currentUser.getUid());
            }
        });
    }

    private void cancelWork() {
        Log.d(TAG,"cancelWork");
        available.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    geoFireAvailable.removeLocation(currentUser.getUid(),
                            new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (error != null) {
                                        Log.d(TAG, error.getMessage(), error.toException());
                                    } else {
                                        Log.d(TAG, "geoFireAvailable.removeLocation :: " +
                                                key);
                                        Log.d(TAG,
                                                "sucessfully removeLocation " +
                                                        "geoFireAvailable :: " + currentUser.getUid());
                                        Toast.makeText(mechanicMapActivity.this, "sucessfully removed " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if (requestCode == SET_PROFILE) {
            updateLocationUI();
        }
    }

    private void findWork() {
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        employees_notified.addValueEventListener(employeeNotificationVE);

//        mLocationRequest = new LocationRequest();
//
//        mLocationRequest.setInterval(1000);
//        mLocationRequest.setFastestInterval(1000);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        Log.d(TAG, "onMapReady");

    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                if ("".equals(employerID)) {
                                    working.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                geoFireWorking.removeLocation(currentUser.getUid(),
                                                        new GeoFire.CompletionListener() {
                                                            @Override
                                                            public void onComplete(String key, DatabaseError error) {
                                                                if (error != null) {
                                                                    Log.d(TAG, error.getMessage(), error.toException());
                                                                } else {
                                                                    Log.d(TAG, "geoFireWorking.removeLocation :: " +
                                                                            key);
                                                                    Log.d(TAG,
                                                                            "sucessfully removed " +
                                                                                    "geoFireWorking :: " + currentUser.getUid());
                                                                    Toast.makeText(mechanicMapActivity.this, "sucessfully removed " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                    geoFireAvailable.setLocation(currentUser.getUid(), new GeoLocation(lastKnownLocation.getLatitude(),
                                            lastKnownLocation.getLongitude()), new GeoFire.CompletionListener() {
                                        @Override
                                        public void onComplete(String key, DatabaseError error) {
                                            if (error != null) {
                                                Log.d(TAG, error.getMessage(), error.toException());
                                            } else {
                                                Log.d(TAG, "geoFireAvailable.setLocation :: " +
                                                        key);
                                                Log.d(TAG,
                                                        "sucessfully setLocation " +
                                                                "geoFireAvailable :: " + currentUser.getUid());
                                                Toast.makeText(mechanicMapActivity.this, "sucessfully setLocation " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                } else {
                                    available.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                geoFireAvailable.removeLocation(currentUser.getUid(),
                                                        new GeoFire.CompletionListener() {
                                                            @Override
                                                            public void onComplete(String key, DatabaseError error) {
                                                                if (error != null) {
                                                                    Log.d(TAG, error.getMessage(), error.toException());
                                                                } else {
                                                                    Log.d(TAG, "geoFireAvailable.removeLocation :: " +
                                                                            key);
                                                                    Log.d(TAG,
                                                                            "sucessfully removeLocation " +
                                                                                    "geoFireAvailable :: " + currentUser.getUid());
                                                                    Toast.makeText(mechanicMapActivity.this, "sucessfully removed " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                    geoFireWorking.setLocation(currentUser.getUid(),
                                            new GeoLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()),
                                            new GeoFire.CompletionListener() {
                                                @Override
                                                public void onComplete(String key, DatabaseError error) {
                                                    if (error != null) {
                                                        Log.d(TAG, error.getMessage(), error.toException());
                                                    } else {
                                                        Log.d(TAG, "geoFireWorking.setLocation :: " +
                                                                key);
                                                        Log.d(TAG,
                                                                "sucessfully setLocation " +
                                                                        "geoFireWorking :: " + currentUser.getUid());
                                                        Toast.makeText(mechanicMapActivity.this, "sucessfully removed " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                }

                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            } else {
                Log.d(TAG, "getDeviceLocation");
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
//                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mWorkingSwitch.setEnabled(true);
                cardView.setEnabled(true);
                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mechanicMapActivity.this));

                FirebaseDatabase.getInstance().getReference("Users").child("Employee")
                        .child(currentUser.getUid())
                        .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (task.isSuccessful()) {
                            mechanic mech =
                                    task.getResult().getValue(mechanic.class);
                            current_user_phone = mech.getPhone();
                            profile_good = mech.getSkill() != null;
                            Log.d(TAG, "updateLocationUI onComplete " + mech.getSkill());
                        }
                    }
                });
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseDatabase.getInstance().getReference("Users").child("Employee")
                .child(currentUser.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    mechanic mech =
                            task.getResult().getValue(mechanic.class);
                    current_user_phone = mech.getPhone();
                    profile_good = mech.getSkill() != null;
                }
            }
        });
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            Log.d(TAG, "locationPermissionGranted");

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
            default:
                Log.d(TAG, "onRequestPermissionsResult");
        }
        updateLocationUI();
    }
}