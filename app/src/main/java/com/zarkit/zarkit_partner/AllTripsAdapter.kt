package com.zarkit.zarkit_partner

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.zarkit.zarkit_partner.api.Trip

class AllTripsAdapter(private val trips: List<Trip>) :
    RecyclerView.Adapter<AllTripsAdapter.TripViewHolder>() {

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvRideId: TextView = view.findViewById(R.id.tvRideId)   // ✅ ADDED

        val tvPickup: TextView = view.findViewById(R.id.tvPickup)
        val tvDrop: TextView = view.findViewById(R.id.tvDrop)
        val tvFare: TextView = view.findViewById(R.id.tvFare)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)

        val tvPaymentStatus: TextView = view.findViewById(R.id.tvPaymentStatus)
        val btnPay: Button = view.findViewById(R.id.btnPay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {

        val trip = trips[position]

        // ✅ SET RIDE ID
        holder.tvRideId.text = "Ride ID: ${trip.id}"

        holder.tvPickup.text = "Pickup: ${trip.pickupAddress ?: ""}"
        holder.tvDrop.text = "Drop: ${trip.dropAddress ?: ""}"
        holder.tvFare.text = "Fare: ₹${trip.finalFare ?: 0}"
        holder.tvDistance.text = trip.distanceText ?: ""

        val status = trip.paymentStatus ?: "PENDING"

        holder.tvPaymentStatus.text = "Payment: $status"

        when (status.uppercase()) {

            "PENDING" -> {
                holder.tvPaymentStatus.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_red_dark)
                )
                holder.btnPay.visibility = View.VISIBLE
            }

            "ONLINE_PAY" -> {
                holder.tvPaymentStatus.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_blue_dark)
                )
                holder.btnPay.visibility = View.GONE
            }

            "CASH" -> {
                holder.tvPaymentStatus.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_green_dark)
                )
                holder.btnPay.visibility = View.GONE
            }

            else -> {
                holder.tvPaymentStatus.setTextColor(
                    holder.itemView.context.getColor(android.R.color.darker_gray)
                )
                holder.btnPay.visibility = View.GONE
            }
        }

        holder.btnPay.setOnClickListener {
            trip.id?.let {
                val intent = Intent(holder.itemView.context, CollectPaymentActivity::class.java)
                intent.putExtra("rideId", it)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = trips.size
}