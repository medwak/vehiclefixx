package com.example.vehiclefixx.users.drivers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.vehiclefixx.MainActivity;
import com.example.vehiclefixx.R;
import com.example.vehiclefixx.data.Garage;
import com.example.vehiclefixx.data.VehicleType;
import com.example.vehiclefixx.data.mechanic;
import com.example.vehiclefixx.services.NearbyGarageFinder;
import com.example.vehiclefixx.services.SendSms;
import com.example.vehiclefixx.settings.CustomInfoWindowAdapter;
import com.example.vehiclefixx.settings.vehicleFixx_Settings;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Driver home screen: finds garages and online mechanics near the driver.
 *
 * <ul>
 *     <li>Location: fused provider {@code getCurrentLocation()} (fresh, high accuracy) with a
 *     fallback to the last known fix, requested through the Activity Result permission API
 *     (fine or approximate location both work).</li>
 *     <li>Garages: Google Places API (New) search, ranked by distance, for the driver's
 *     vehicle type (car, motorbike or lorry), so real garages show up even if they never
 *     registered in the app.</li>
 *     <li>Mechanics: a GeoFire radius query on {@code employees_available} instead of downloading
 *     every mechanic and filtering on the phone.</li>
 *     <li>Search elsewhere: Places autocomplete moves the search to any place the driver picks.</li>
 * </ul>
 */
public class driverMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "driverMapActivity";

    // Google Maps zoom levels run from ~2 (world) to ~21 (buildings).
    private static final float CITY_ZOOM = 13f;
    private static final float STREET_ZOOM = 15f;
    private static final float CLOSE_ZOOM = 17f;

    /** Garage search radius; Places Nearby Search allows up to 50 km. */
    private static final double GARAGE_RADIUS_METERS = 10_000;
    /** Mechanic GeoFire search radius in km. */
    private static final double MECHANIC_RADIUS_KM = 10;
    /** A mechanic closer than this is "here". */
    private static final float ARRIVED_METERS = 100;
    /** Accept a cached fix up to this old before asking for a new one. */
    private static final long MAX_LOCATION_AGE_MS = 60_000;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final int RATE_REQUEST = 1254;
    private static final String PREFS = "driver_map";
    private static final String PREF_VEHICLE_TYPE = "vehicle_type";

    private final Gson gson = new Gson();
    private final Map<String, Marker> garageMarkers = new HashMap<>();
    private final Map<String, Marker> mechanicMarkers = new HashMap<>();

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource locationCancellation;
    private NearbyGarageFinder garageFinder;

    private FirebaseUser currentUser;
    private DatabaseReference employeesAvailable;
    private DatabaseReference employeesNotified;
    private DatabaseReference ratingsRef;
    private GeoQuery mechanicQuery;

    @Nullable private Location lastKnownLocation;
    @Nullable private CameraPosition savedCameraPosition;
    @Nullable private LatLng pickupLocation;
    @Nullable private Marker searchCenterMarker;
    /** Where the last garage search ran, so changing vehicle type can re-run it there. */
    @Nullable private LatLng lastSearchCenter;
    @NonNull private VehicleType vehicleType = VehicleType.CAR;
    private String requestService;
    private boolean searchingMechanics = false;
    private boolean locationPermissionGranted = false;

    private Button requestBtn;
    private Button findGaragesBtn;
    private RadioGroup radioGroup;
    private RadioGroup vehicleTypeGroup;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        // Approximate (coarse) location is enough to find nearby garages.
                        locationPermissionGranted =
                                Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                        updateLocationUI();
                        if (locationPermissionGranted) {
                            findGaragesNearMe();
                        } else {
                            Toast.makeText(this, R.string.location_permission_needed,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            savedCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        // Must run before inflation: the layout hosts the Places autocomplete fragment.
        NearbyGarageFinder.initPlaces(this);
        setContentView(R.layout.activity_driver_map);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setTitle(currentUser.getDisplayName());

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        employeesAvailable = db.getReference("employees_available");
        employeesNotified = db.getReference("employees_notified").child(currentUser.getUid());
        ratingsRef = db.getReference("Ratings");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        garageFinder = new NearbyGarageFinder(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        requestBtn = findViewById(R.id.request);
        findGaragesBtn = findViewById(R.id.find_garages);
        radioGroup = findViewById(R.id.radioGroup);
        vehicleTypeGroup = findViewById(R.id.vehicle_type_group);
        setUpVehicleTypePicker();

        findViewById(R.id.logout).setOnClickListener(v -> {
            stopMechanicSearch();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.settings).setOnClickListener(v ->
                startActivity(new Intent(this, driverSettingsActivity.class)));
        findGaragesBtn.setOnClickListener(v -> findGaragesNearMe());
        requestBtn.setOnClickListener(v -> {
            if (searchingMechanics) {
                stopMechanicSearch();
            } else {
                requestMechanic();
            }
        });

        setUpPlaceSearch();
    }

    /**
     * Car / motorbike / lorry picker. The choice is remembered and changing it re-runs the
     * garage search at the same spot.
     */
    private void setUpVehicleTypePicker() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        vehicleType = VehicleType.fromName(prefs.getString(PREF_VEHICLE_TYPE, null));
        vehicleTypeGroup.check(radioIdFor(vehicleType));
        vehicleTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            VehicleType selected = vehicleTypeFor(checkedId);
            if (selected == vehicleType) return;
            vehicleType = selected;
            prefs.edit().putString(PREF_VEHICLE_TYPE, selected.name()).apply();
            if (lastSearchCenter != null) searchGaragesAround(lastSearchCenter);
        });
    }

    private static int radioIdFor(@NonNull VehicleType type) {
        switch (type) {
            case MOTORBIKE:
                return R.id.vehicle_motorbike;
            case LORRY:
                return R.id.vehicle_lorry;
            case CAR:
            default:
                return R.id.vehicle_car;
        }
    }

    @NonNull
    private static VehicleType vehicleTypeFor(int radioId) {
        // if/else, not switch: R.id values are not compile-time constants under AGP 8.
        if (radioId == R.id.vehicle_motorbike) return VehicleType.MOTORBIKE;
        if (radioId == R.id.vehicle_lorry) return VehicleType.LORRY;
        return VehicleType.CAR;
    }

    @NonNull
    private String vehicleLabel() {
        return getString(vehicleType.getLabel()).toLowerCase(Locale.getDefault());
    }

    /** Lets the driver search garages around any address / landmark, not only their GPS fix. */
    private void setUpPlaceSearch() {
        AutocompleteSupportFragment autocomplete = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        if (autocomplete == null) return;
        autocomplete.setPlaceFields(Arrays.asList(
                Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION));
        autocomplete.setHint(getString(R.string.search_garages_hint));
        autocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLocation();
                if (latLng == null) return;
                showSearchCenter(latLng, place.getDisplayName());
                searchGaragesAround(latLng);
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.w(TAG, "Place autocomplete error: " + status);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));
        mMap.setOnInfoWindowClickListener(marker ->
                Toast.makeText(this, R.string.long_press_options, Toast.LENGTH_SHORT).show());
        mMap.setOnInfoWindowLongClickListener(this::onMarkerLongPressed);

        if (savedCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(savedCameraPosition));
        }
        ensureLocationPermission();
    }

    // ---------------------------------------------------------------------------------------
    // Location
    // ---------------------------------------------------------------------------------------

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureLocationPermission() {
        locationPermissionGranted = hasLocationPermission();
        if (locationPermissionGranted) {
            updateLocationUI();
            findGaragesNearMe();
        } else {
            // Android 12+ shows the precise / approximate choice for this pair.
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    @SuppressLint("MissingPermission") // checked via locationPermissionGranted
    private void updateLocationUI() {
        if (mMap == null) return;
        mMap.setMyLocationEnabled(locationPermissionGranted);
        mMap.getUiSettings().setMyLocationButtonEnabled(locationPermissionGranted);
        requestBtn.setEnabled(locationPermissionGranted);
        findGaragesBtn.setEnabled(locationPermissionGranted);
        if (!locationPermissionGranted) lastKnownLocation = null;
    }

    private interface LocationCallback {
        void onLocation(@NonNull Location location);
    }

    /**
     * Gets a fresh fix with {@code getCurrentLocation()} (the recommended replacement for
     * relying on {@code getLastLocation()}, which is often null), falling back to the last fix.
     */
    @SuppressLint("MissingPermission") // checked via hasLocationPermission()
    private void fetchCurrentLocation(@NonNull LocationCallback callback) {
        if (!hasLocationPermission()) {
            ensureLocationPermission();
            return;
        }
        if (!vehicleFixx_Settings.isLocationEnabled(this)) {
            Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        if (locationCancellation != null) locationCancellation.cancel();
        locationCancellation = new CancellationTokenSource();

        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
                .setDurationMillis(15_000)
                .build();

        fusedLocationClient.getCurrentLocation(request, locationCancellation.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastKnownLocation = location;
                        callback.onLocation(location);
                        return;
                    }
                    fusedLocationClient.getLastLocation().addOnSuccessListener(last -> {
                        if (last != null) {
                            lastKnownLocation = last;
                            callback.onLocation(last);
                        } else {
                            Toast.makeText(this, R.string.location_unavailable,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getCurrentLocation failed", e);
                    Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_LONG).show();
                });
    }

    private static LatLng toLatLng(@NonNull Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    // ---------------------------------------------------------------------------------------
    // Garages (Google Places Nearby Search)
    // ---------------------------------------------------------------------------------------

    private void findGaragesNearMe() {
        fetchCurrentLocation(location -> {
            if (searchCenterMarker != null) {
                searchCenterMarker.remove();
                searchCenterMarker = null;
            }
            searchGaragesAround(toLatLng(location));
        });
    }

    private void showSearchCenter(@NonNull LatLng latLng, @Nullable String title) {
        if (mMap == null) return;
        if (searchCenterMarker != null) searchCenterMarker.remove();
        searchCenterMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    private void searchGaragesAround(@NonNull LatLng center) {
        if (mMap == null) return;
        lastSearchCenter = center;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, CITY_ZOOM));
        findGaragesBtn.setEnabled(false);
        findGaragesBtn.setText(R.string.finding_garages);

        final VehicleType searchedType = vehicleType;
        garageFinder.findGarages(center, GARAGE_RADIUS_METERS, searchedType,
                new NearbyGarageFinder.Callback() {
            @Override
            public void onGaragesFound(@NonNull List<Garage> garages) {
                if (isFinishing() || isDestroyed()) return;
                // Ignore a stale answer if the driver switched vehicle type meanwhile.
                if (searchedType != vehicleType) return;
                resetGarageButton();
                showGarages(center, garages);
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (isFinishing() || isDestroyed()) return;
                resetGarageButton();
                Toast.makeText(driverMapActivity.this, R.string.garage_search_failed,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetGarageButton() {
        findGaragesBtn.setEnabled(locationPermissionGranted);
        findGaragesBtn.setText(R.string.garages_near_me);
    }

    private void showGarages(@NonNull LatLng center, @NonNull List<Garage> garages) {
        for (Marker m : garageMarkers.values()) m.remove();
        garageMarkers.clear();

        if (garages.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_garages_found, vehicleLabel(),
                    Garage.formatDistance((float) GARAGE_RADIUS_METERS)), Toast.LENGTH_LONG).show();
            return;
        }

        LatLngBounds.Builder bounds = new LatLngBounds.Builder().include(center);
        for (Garage garage : garages) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(garage.getLocation())
                    .title(garage.getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.gaicon)));
            if (marker == null) continue;
            marker.setTag(garage);
            garageMarkers.put(garage.getPlaceId(), marker);
        }
        // Frame the user plus the closest few garages so the nearest one is obvious.
        for (int i = 0; i < Math.min(5, garages.size()); i++) {
            bounds.include(garages.get(i).getLocation());
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 150));

        Garage nearest = garages.get(0);
        Marker nearestMarker = garageMarkers.get(nearest.getPlaceId());
        if (nearestMarker != null) nearestMarker.showInfoWindow();
        Toast.makeText(this, getString(R.string.garages_found, garages.size(), vehicleLabel(),
                nearest.getFormattedDistance()), Toast.LENGTH_SHORT).show();
    }

    private void showGarageActions(@NonNull Garage garage) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(garage.getName())
                .setMessage(garage.getAddress() != null ? garage.getAddress() : "")
                .setIcon(android.R.drawable.ic_dialog_map)
                .setPositiveButton(R.string.directions, (d, w) -> openDirections(garage))
                .setNegativeButton(R.string.Cancel, null);
        if (garage.getPhone() != null) {
            dialog.setNeutralButton(R.string.call, (d, w) -> startActivity(
                    new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + garage.getPhone()))));
        }
        dialog.show();
    }

    /** Opens turn-by-turn directions in Google Maps (or any maps app / browser). */
    private void openDirections(@NonNull Garage garage) {
        LatLng dest = garage.getLocation();
        Uri uri = Uri.parse("https://www.google.com/maps/dir/?api=1"
                + "&destination=" + dest.latitude + "," + dest.longitude
                + "&destination_place_id=" + Uri.encode(garage.getPlaceId())
                + "&travelmode=driving");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.garage_search_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Registered mechanics (GeoFire radius query)
    // ---------------------------------------------------------------------------------------

    private void requestMechanic() {
        RadioButton radioButton = findViewById(radioGroup.getCheckedRadioButtonId());
        if (radioButton == null || radioButton.getText() == null) {
            vehicleFixx_Settings.showAlert(this, "Select Category", "FreeLance or Repairs");
            return;
        }
        requestService = radioButton.getText().toString();
        fetchCurrentLocation(location -> startMechanicSearch(toLatLng(location)));
    }

    private void startMechanicSearch(@NonNull LatLng pickup) {
        stopMechanicSearch();
        searchingMechanics = true;
        pickupLocation = pickup;
        requestBtn.setText(R.string.searching);
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickup, STREET_ZOOM));
        }

        // Ask the database only for mechanics inside the radius, and get live
        // enter / move / exit updates as they go online, drive around or go offline.
        mechanicQuery = new GeoFire(employeesAvailable)
                .queryAtLocation(new GeoLocation(pickup.latitude, pickup.longitude), MECHANIC_RADIUS_KM);
        mechanicQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                loadMechanic(key, new LatLng(location.latitude, location.longitude));
            }

            @Override
            public void onKeyExited(String key) {
                Marker marker = mechanicMarkers.remove(key);
                if (marker != null) marker.remove();
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                LatLng latLng = new LatLng(location.latitude, location.longitude);
                Marker marker = mechanicMarkers.get(key);
                if (marker != null) {
                    marker.setPosition(latLng);
                    updateMechanicStatus(latLng);
                }
            }

            @Override
            public void onGeoQueryReady() {
                if (searchingMechanics && mechanicMarkers.isEmpty()) {
                    requestBtn.setText(R.string.no_mechanics_nearby);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "GeoQuery error", error.toException());
            }
        });
    }

    private void loadMechanic(@NonNull String id, @NonNull LatLng latLng) {
        FirebaseDatabase.getInstance().getReference("Users").child("Employee").child(id).get()
                .addOnSuccessListener(snapshot -> {
                    mechanic mech = snapshot.getValue(mechanic.class);
                    if (!searchingMechanics || mech == null) return;
                    if (requestService != null && !requestService.equals(mech.getService())) return;
                    if (mech.getId() == null) mech.setId(id);
                    loadRatingThenShow(id, mech, latLng);
                })
                .addOnFailureListener(e -> Log.e(TAG, "load mechanic " + id, e));
    }

    private void loadRatingThenShow(@NonNull String id, @NonNull mechanic mech,
                                    @NonNull LatLng latLng) {
        ratingsRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float rate = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if ("rate".equals(child.getKey()) && child.getValue() != null) {
                        try {
                            rate += Float.parseFloat(child.getValue().toString());
                        } catch (NumberFormatException ignored) {
                            // skip malformed rating
                        }
                    }
                }
                mech.setRatings(String.valueOf(rate));
                showMechanic(id, mech, latLng);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showMechanic(id, mech, latLng);
            }
        });
    }

    private void showMechanic(@NonNull String id, @NonNull mechanic mech, @NonNull LatLng latLng) {
        if (!searchingMechanics || mMap == null) return;
        Marker old = mechanicMarkers.remove(id);
        if (old != null) old.remove();
        // The Marker field is not serialisable; keep it out of the JSON snippet.
        mech.setMarker(null);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(mech.getName())
                .snippet(gson.toJson(mech))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.proicon)));
        if (marker == null) return;
        marker.setTag(mech);
        mechanicMarkers.put(id, marker);
        updateMechanicStatus(nearestMechanic());
    }

    @Nullable
    private LatLng nearestMechanic() {
        if (pickupLocation == null) return null;
        LatLng best = null;
        float bestDist = Float.MAX_VALUE;
        for (Marker m : mechanicMarkers.values()) {
            float d = NearbyGarageFinder.distanceBetween(pickupLocation, m.getPosition());
            if (d < bestDist) {
                bestDist = d;
                best = m.getPosition();
            }
        }
        return best;
    }

    private void updateMechanicStatus(@Nullable LatLng mechanicLatLng) {
        if (!searchingMechanics || pickupLocation == null || mechanicLatLng == null) return;
        float distance = NearbyGarageFinder.distanceBetween(pickupLocation, mechanicLatLng);
        if (distance < ARRIVED_METERS) {
            requestBtn.setText(R.string.mechanic_here);
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, CLOSE_ZOOM));
            }
        } else {
            requestBtn.setText(getString(R.string.mechanic_found, Garage.formatDistance(distance)));
        }
    }

    private void stopMechanicSearch() {
        if (mechanicQuery != null) {
            mechanicQuery.removeAllListeners();
            mechanicQuery = null;
        }
        for (Marker m : mechanicMarkers.values()) m.remove();
        mechanicMarkers.clear();
        searchingMechanics = false;
        if (requestBtn != null) {
            requestBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.button_green));
            requestBtn.setText(R.string.search_mechanic);
        }
    }

    private void showHireDialog(@NonNull mechanic mech, @NonNull Marker marker) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.hire_mechanic_title)
                .setMessage(R.string.hire_mechanic_message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    String name = currentUser.getDisplayName() != null
                            ? currentUser.getDisplayName() : "";
                    // Runs in the background; SendSms reports its own progress / result.
                    new SendSms(this, name, currentUser.getUid(), mech.getId(),
                            pickupLocation, employeesNotified, marker).execute(mech);
                })
                .setNegativeButton(android.R.string.no, null)
                .setNeutralButton(R.string.rate, (dialog, which) -> {
                    Intent intent = new Intent(this, mechanicRateActivity.class);
                    intent.putExtra("mechanic", mech.getId());
                    intent.putExtra("driver", currentUser.getUid());
                    startActivityForResult(intent, RATE_REQUEST);
                })
                .show();
    }

    // ---------------------------------------------------------------------------------------
    // Markers & lifecycle
    // ---------------------------------------------------------------------------------------

    private void onMarkerLongPressed(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof Garage) {
            showGarageActions((Garage) tag);
        } else if (tag instanceof mechanic) {
            showHireDialog((mechanic) tag, marker);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (locationCancellation != null) locationCancellation.cancel();
        if (mechanicQuery != null) mechanicQuery.removeAllListeners();
        super.onDestroy();
    }
}
