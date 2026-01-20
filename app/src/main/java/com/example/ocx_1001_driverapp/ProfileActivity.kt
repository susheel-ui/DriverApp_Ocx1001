package com.example.ocx_1001_driverapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val txtName = findViewById<TextView>(R.id.txtName)
        val txtPhone = findViewById<TextView>(R.id.txtPhone)
        val txtRole = findViewById<TextView>(R.id.txtRole)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Load from LocalStorage
        txtName.text = "Driver ID: ${LocalStorage.getUserId(this)}"
        txtPhone.text = LocalStorage.getPhone(this) ?: "-"
        txtRole.text = LocalStorage.getRole(this) ?: "DRIVER"

        btnBack.setOnClickListener { finish() }
    }
}
