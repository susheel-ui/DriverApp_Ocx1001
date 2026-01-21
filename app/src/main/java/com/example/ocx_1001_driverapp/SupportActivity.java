package com.example.ocx_1001_driverapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public class SupportActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dynamically get layout ID without R
        int layoutId = getResources().getIdentifier(
                "activity_support", // XML file name in res/layout
                "layout",
                getPackageName()
        );

        // Inflate layout
        View view = LayoutInflater.from(this).inflate(layoutId, null);
        setContentView(view);
    }
}
