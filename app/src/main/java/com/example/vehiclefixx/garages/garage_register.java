package com.example.vehiclefixx.garages;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vehiclefixx.R;
import com.example.vehiclefixx.users.drivers.driverMapActivity;
import com.example.vehiclefixx.users.mechanics.mechanicRegistrationActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class garage_register extends AppCompatActivity  {
    private static final String TAG =
            "TAG :: " + garage_register.class.getSimpleName() + " :: ";
    private EditText registerEmail, registerPassword, registerName, registerPhone;
    private FirebaseAuth mAuth;
    private ProgressDialog loader;
    private DatabaseReference userDatabaseRef;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");
        setContentView(R.layout.activity_garage_register);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setTitle(getResources().getString(R.string.garage_registration_title));
        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "firebaseAuthListener :: onAuthStateChanged ");
                    Intent intent = new Intent(garage_register.this,
                            garage_login.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        loader = new ProgressDialog(this);
        registerEmail = findViewById(R.id.registerEmail);
        registerPassword = findViewById(R.id.registerPassword);
        registerName = findViewById(R.id.registerFull_name);
        registerPhone = findViewById(R.id.registerPhone);
        Button login_btn = findViewById(R.id.login_btn);
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(garage_register.this,
                        garage_login.class));
            }
        });
        Button register_btn = findViewById(R.id.register_btn);
        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });
        TextView as_an_garage = findViewById(R.id.as_an_garage);
        as_an_garage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(garage_register.this,
                        garage_register.class));
            }
        });
    }

    private void performRegistration() {
        final String email = registerEmail.getText().toString();
        final String password = registerPassword.getText().toString();
        final String name = registerName.getText().toString();
        final String phone = registerPhone.getText().toString();
        if (TextUtils.isEmpty(name)) {
            registerName.setError("Name Required!");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            registerPhone.setError("Username Required!");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            registerEmail.setError("Email Required!");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            registerPassword.setError("Password Required!");
            return;
        }
        loader.setMessage("Registration in progress...");
        loader.setCanceledOnTouchOutside(false);
        loader.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(garage_register.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            String error = task.getException().toString();
                            Toast.makeText(garage_register.this, "Registration Failed: \n" + error, Toast.LENGTH_SHORT).show();
                            loader.dismiss();
                            Log.e(TAG, "Registration Failed: "+task.getException().getMessage(),
                                    task.getException());
                            if(task.getException().getMessage().contains("The email address is badly formatted.")){
                                registerEmail.setError("Email address is badly formatted.");
                            }
                        } else {
                            String currentUserId = mAuth.getCurrentUser().getUid();
                            FirebaseUser user = mAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "User profile updated.isSuccessful");
                                        }
                                    })
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG,
                                                        "User profile updated." + task.getResult());
                                                Log.d(TAG, "User profile updated. isSuccessful");
                                            }else{
                                                Log.e(TAG, Objects.requireNonNull(task.getException()).getMessage(),
                                                        task.getException());
                                            }
                                        }
                                    });

                            userDatabaseRef =
                                    FirebaseDatabase.getInstance().getReference("Users").child(
                                            "Employer").child(currentUserId);
                            HashMap<String, Object> userInfo = new HashMap<>();
                            userInfo.put("id", currentUserId);
                            userInfo.put("name", name);
                            userInfo.put("phone", phone);
                            userInfo.put("email", email);
                            userInfo.put("type", "employer");
                            userDatabaseRef
                                    .updateChildren(userInfo)
                                    .addOnCompleteListener(garage_register.this,
                                            new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(garage_register.this, "your have successfully registered", Toast.LENGTH_SHORT).show();
                                                        Log.e(TAG, "onCancelled", task.getException());
                                                        loader.dismiss();
                                                        Intent intent =
                                                                new Intent(garage_register.this, driverMapActivity.class);
                                                        startActivity(intent);
                                                        finish();

                                                    } else {
                                                        String error = task.getException().toString();
                                                        Log.e(TAG, task.getException().getMessage(),
                                                                task.getException());
                                                        task.getException().printStackTrace();
                                                        loader.dismiss();
                                                        Toast.makeText(garage_register.this, "Details upload Failed: " + error, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                            loader.dismiss();
                        }
                    }
                });
    }
}