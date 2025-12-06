package com.example.ocx_1001_driverapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RideRequestPopupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_popup)

        val txtMessage = findViewById<TextView>(R.id.txtMessage)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnReject = findViewById<Button>(R.id.btnReject)

        txtMessage.text = "New Ride Request Available"

        btnAccept.setOnClickListener {
            finish()
        }

        btnReject.setOnClickListener {
            finish()
        }
    }
}
