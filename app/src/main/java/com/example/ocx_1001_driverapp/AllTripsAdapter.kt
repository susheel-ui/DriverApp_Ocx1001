package com.example.ocx_1001_driverapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ocx_1001_driverapp.api.Trip

class AllTripsAdapter(private val trips: List<Trip>) :
    RecyclerView.Adapter<AllTripsAdapter.TripViewHolder>() {

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPickup: TextView = view.findViewById(R.id.tvPickup)
        val tvDrop: TextView = view.findViewById(R.id.tvDrop)
        val tvFare: TextView = view.findViewById(R.id.tvFare)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]
        holder.tvPickup.text = "Pickup: ${trip.pickupAddress}"
        holder.tvDrop.text = "Drop: ${trip.dropAddress}"
        holder.tvFare.text = "Fare: ₹${trip.finalFare}"
        holder.tvDistance.text = "Distance: ${trip.distanceText}"
    }

    override fun getItemCount(): Int = trips.size
}
