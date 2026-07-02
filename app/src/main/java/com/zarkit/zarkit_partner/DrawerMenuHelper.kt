package com.zarkit.zarkit_partner

import android.content.Intent
import android.util.Log
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.ActiveRideResponse

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

        // ================= CURRENT TRIP =================
        menuCurrentTrip.setOnClickListener {

            drawerLayout.closeDrawers()

            val activeRideId = LocalStorage.getActiveRideId(activity)

            Log.d("DRAWER_CURRENT_TRIP", "Clicked | activeRideId = $activeRideId")

            if (activeRideId > 0L) {
                openLiveRide(activity, activeRideId)
            } else {
                fetchActiveRideFromApi(activity)
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
        menuLogout.isClickable = true
        menuLogout.isFocusable = true
        menuLogout.setOnClickListener {
            drawerLayout.closeDrawers()
            onLogout()
        }
    }

    // ================= HELPERS =================

    private fun openLiveRide(activity: DashboardActivity, rideId: Long) {
        val intent = Intent(activity, LiveRideActivity::class.java).apply {
            putExtra("rideId", rideId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        activity.startActivity(intent)
    }

    private fun fetchActiveRideFromApi(activity: DashboardActivity) {

        val token = LocalStorage.getToken(activity) ?: run {
            activity.showPopup("No Active Ride", "You do not have any active ride at the moment.")
            return
        }

        val driverId = LocalStorage.getUserId(activity)

        ApiClient.api.getLatestActiveRide(
            "Bearer $token",
            driverId
        ).enqueue(object : Callback<ActiveRideResponse> {

            override fun onResponse(
                call: Call<ActiveRideResponse>,
                response: Response<ActiveRideResponse>
            ) {
                val rideId = response.body()?.rideId ?: 0L

                if (rideId > 0L) {
                    LocalStorage.saveActiveRideId(activity, rideId)
                    openLiveRide(activity, rideId)
                } else {
                    activity.showPopup("No Active Ride", "You do not have any active ride at the moment.")
                }
            }

            override fun onFailure(call: Call<ActiveRideResponse>, t: Throwable) {
                activity.showPopup("No Active Ride", "You do not have any active ride at the moment.")
            }
        })
    }
}