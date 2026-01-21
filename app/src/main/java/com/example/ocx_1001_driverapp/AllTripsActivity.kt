package com.example.ocx_1001_driverapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocx_1001_driverapp.api.ApiClient
import com.example.ocx_1001_driverapp.api.Trip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AllTripsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_trips)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerTrips)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnBack.setOnClickListener { finish() }

        fetchTrips(recyclerView)
    }

    private fun fetchTrips(recyclerView: RecyclerView) {
        // Fetch user ID and token from LocalStorage
        val driverId = LocalStorage.getUserId(this)
        val token = LocalStorage.getToken(this)

        if (token.isNullOrEmpty() || driverId == 0L) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val authHeader = "Bearer $token"

        ApiClient.api.getAllTrips(driverId.toInt(), authHeader)
            .enqueue(object : Callback<List<Trip>> {
                override fun onResponse(call: Call<List<Trip>>, response: Response<List<Trip>>) {
                    if (response.isSuccessful) {
                        val trips = response.body() ?: emptyList()
                        if (trips.isEmpty()) {
                            Toast.makeText(this@AllTripsActivity, "No trips found", Toast.LENGTH_SHORT).show()
                        }
                        recyclerView.adapter = AllTripsAdapter(trips)
                    } else {
                        Toast.makeText(this@AllTripsActivity, "Failed to load trips", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Trip>>, t: Throwable) {
                    Toast.makeText(this@AllTripsActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
