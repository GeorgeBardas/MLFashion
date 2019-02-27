package com.mlfashion.mlfashion;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class MLFashion extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(getApplicationContext());
    }
}
