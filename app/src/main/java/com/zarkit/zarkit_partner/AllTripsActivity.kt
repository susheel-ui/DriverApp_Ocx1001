package com.zarkit.zarkit_partner

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.Trip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AllTripsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_trips)

        // ✅ Apply edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { view, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

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
