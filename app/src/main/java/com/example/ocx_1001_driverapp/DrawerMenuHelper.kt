package com.example.ocx_1001_driverapp

import android.content.Intent
import android.util.Log
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ocx_1001_driverapp.AllTripsActivity
import com.example.ocx_1001_driverapp.UserProfileActivity

object DrawerMenuHelper {

    fun setupDrawerMenu(
        drawerLayout: DrawerLayout,
        activity: DashboardActivity,
        onLogout: () -> Unit
    ) {

        val menuProfile = drawerLayout.findViewById<TextView>(R.id.menuProfile)
        val menuCurrentTrip = drawerLayout.findViewById<TextView>(R.id.menuCurrentTrip)
        val menuAllTrips = drawerLayout.findViewById<TextView>(R.id.menuAllTrips)
        val menuSupport = drawerLayout.findViewById<TextView>(R.id.menuHistory)
        val menuLogout = drawerLayout.findViewById<TextView>(R.id.menuLogout)

        // PROFILE
        menuProfile.setOnClickListener {
            drawerLayout.closeDrawers()
            activity.startActivity(Intent(activity, UserProfileActivity::class.java))
        }

        // CURRENT TRIP
        menuCurrentTrip.setOnClickListener {
            Log.d("DRAWER_DEBUG", "Current Trip clicked")
            drawerLayout.closeDrawers()

            // Check if active ride exists
            val activeRideId = activity.getActiveRideId()
            if (activeRideId != 0L) {
                activity.startActivity(Intent(activity, LiveRideActivity::class.java))
            } else {
                activity.showPopup("No Active Ride", "You do not have any active ride at the moment.")
            }
        }

        // ALL TRIPS
        menuAllTrips.setOnClickListener {
            drawerLayout.closeDrawers()
            activity.startActivity(Intent(activity, AllTripsActivity::class.java))
        }

        // SUPPORT
        menuSupport.setOnClickListener {
            drawerLayout.closeDrawers()
            activity.startActivity(Intent(activity, SupportActivity::class.java))
        }

        // LOGOUT
        menuLogout.setOnClickListener {
            drawerLayout.closeDrawers()
            onLogout()
        }
    }
}
