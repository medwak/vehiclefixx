package com.example.vehiclefixx.services;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.vehiclefixx.data.mechanic;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

public class FetchMechanicAsync extends AsyncTask<Void, Void, Void> {
    private static final String TAG = FetchMechanicAsync.class.getSimpleName() + " :: ";
    private DatabaseReference employee;
    private TextView name, skill, service, phone;
    private RatingBar ratingBar;
    private Button confirm_btn;
    private ProgressDialog loader;

    public FetchMechanicAsync(Context context, TextView name, TextView phone, TextView skill,
                              TextView service,
                              RatingBar ratingBar, Button confirm_btn, DatabaseReference employee) {
        this.confirm_btn = confirm_btn;
        this.employee = employee;
        this.name = name;
        this.phone = phone;
        this.ratingBar = ratingBar;
        this.service = service;
        this.skill = skill;
        loader = new ProgressDialog(context);
        loader.setMessage("Wait ...");
        loader.setCanceledOnTouchOutside(false);
        loader.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        employee.get()
                .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Task" +
                                    "<DataSnapshot> " +
                                    "task. " +
                                    "isSuccessful");
                            DataSnapshot dataSnapshot1 = task.getResult();
                            Log.d(TAG, "dataSnapshot1" +
                                    " getKey :: " + dataSnapshot1.getKey());
                            Log.d(TAG, "dataSnapshot1" +
                                    " getValue :: " + dataSnapshot1.getValue());
                            mechanic mech =
                                    task.getResult().getValue(mechanic.class);
                            if (mech != null) {
                                Log.d(TAG,
                                        "mechanic :: " + mech.getName());
                                name.setText(mech.getName());
                                Log.d(TAG,
                                        "mechanic :: " + mech.getPhone());
                                phone.setText(mech.getPhone());
                                Log.d(TAG,
                                        "mechanic :: " + mech.getSkill());
                                service.setText(mech.getService());
                                skill.setText(mech.getSkill());
                                confirm_btn.setEnabled(true);
//                                loader.dismiss();
                            } else Log.d(TAG,
                                    "onComplete :: mech == null");
                        } else if (task.getException() != null) {
                            Log.d(TAG, "onComplete :: " + task.getException().getMessage(),
                                    task.getException().getCause());
                        } else {
                            Log.d(TAG, "onComplete :: ERROR");
                        }
                    }
                });
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        this.confirm_btn.setEnabled(true);
        loader.dismiss();
    }
}
