package com.zarkit.zarkit_partner

import android.content.Intent
import android.util.Log
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout

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

        // ================= PROFILE =================
        menuProfile.setOnClickListener {
            drawerLayout.closeDrawers()
            activity.startActivity(
                Intent(activity, UserProfileActivity::class.java)
            )
        }

        // ================= CURRENT TRIP (FIXED 🔥) =================
        menuCurrentTrip.setOnClickListener {

            drawerLayout.closeDrawers()

            // 🔥 READ FROM YOUR EXISTING LocalStorage
            val activeRideId = LocalStorage.getActiveRideId(activity)

            Log.d(
                "DRAWER_CURRENT_TRIP",
                "Clicked | activeRideId = $activeRideId"
            )

            // ✅ ACTIVE ONLY IF > 0
            if (activeRideId > 0L) {

                val intent = Intent(activity, LiveRideActivity::class.java).apply {
                    putExtra("rideId", activeRideId) // 🔥 CRITICAL
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                activity.startActivity(intent)

            } else {

                activity.showPopup(
                    "No Active Ride",
                    "You do not have any active ride at the moment."
                )
            }
        }

        // ================= ALL TRIPS =================
        menuAllTrips.setOnClickListener {
            drawerLayout.closeDrawers()
            activity.startActivity(
                Intent(activity, AllTripsActivity::class.java)
            )
        }

        // ================= SUPPORT =================
        menuSupport.setOnClickListener {
            drawerLayout.closeDrawers()
            activity.startActivity(
                Intent(activity, SupportActivity::class.java)
            )
        }

        // ================= LOGOUT =================
        menuLogout.setOnClickListener {
            drawerLayout.closeDrawers()
            onLogout()
        }
    }
}
