package com.example.ocx_1001_driverapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private var isOnline = false  // default OFFLINE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val goOnlineLayout = findViewById<RelativeLayout>(R.id.btnGoOnline)
        val sliderCircle = findViewById<ImageView>(R.id.sliderCircle)
        val txtStatus = findViewById<TextView>(R.id.txtOnlineStatus)
        val statusDot = findViewById<ImageView>(R.id.statusDot)

        // Default OFFLINE state
        goOnlineLayout.setBackgroundResource(R.drawable.btn_offline)
        txtStatus.text = "GO ONLINE"
        statusDot.setBackgroundResource(R.drawable.status_dot_red)

        goOnlineLayout.setOnClickListener {

            val moveX = goOnlineLayout.width - sliderCircle.width - 12

            if (!isOnline) {

                // ---------------- ONLINE ----------------
                sliderCircle.animate()
                    .translationX(moveX.toFloat())
                    .setDuration(250)
                    .start()

                goOnlineLayout.setBackgroundResource(R.drawable.btn_online)
                txtStatus.text = "GO OFFLINE"
                statusDot.setBackgroundResource(R.drawable.status_dot_green)

                isOnline = true

            } else {

                // ---------------- OFFLINE ----------------
                sliderCircle.animate()
                    .translationX(0f)
                    .setDuration(250)
                    .start()

                goOnlineLayout.setBackgroundResource(R.drawable.btn_offline)
                txtStatus.text = "GO ONLINE"
                statusDot.setBackgroundResource(R.drawable.status_dot_red)

                isOnline = false
            }
        }
    }
}
