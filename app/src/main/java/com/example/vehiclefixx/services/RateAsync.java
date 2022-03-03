package com.example.vehiclefixx.services;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.vehiclefixx.users.drivers.mechanicRateActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

public class RateAsync extends AsyncTask<Void, Void, DatabaseReference> {
    private static final String TAG = FetchAsync.class.getSimpleName() + " :: ";
    private DatabaseReference databaseReference;
    private ProgressDialog loader;
    private HashMap<String, Object> userInfo;
    private mechanicRateActivity mechRateActivity;
    public RateAsync(mechanicRateActivity mechRateActivity, HashMap<String, Object> userInfo, DatabaseReference databaseReference) {
        this.mechRateActivity = mechRateActivity;
        this.databaseReference = databaseReference;
        this.userInfo = userInfo;
    }

    @Override
    protected DatabaseReference doInBackground(Void... voids) {
        databaseReference.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Log.d(TAG, "Task" +
                            "<DataSnapshot> " +
                            "task. " +
                            "isSuccessful");
//                    loader.dismiss();
                    mechRateActivity.finish();
                }else{
                    Log.d(TAG, "Task" +
                            "<DataSnapshot> " +
                            "task. " +
                            "not Success");
                }
            }
        });
        return null;
    }

    @Override
    protected void onPostExecute(DatabaseReference databaseReference) {
        super.onPostExecute(databaseReference);
    }
}
