package com.zarkit.zarkit_partner

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zarkit.zarkit_partner.api.Trip
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AllTripsAdapter(private val trips: List<Trip>) :
    RecyclerView.Adapter<AllTripsAdapter.TripViewHolder>() {

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvRideId: TextView = view.findViewById(R.id.tvRideId)
        val tvRideStatus: TextView = view.findViewById(R.id.tvRideStatus)

        val tvPickup: TextView = view.findViewById(R.id.tvPickup)
        val tvDrop: TextView = view.findViewById(R.id.tvDrop)
        val tvFare: TextView = view.findViewById(R.id.tvFare)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)

        val tvPaymentStatus: TextView = view.findViewById(R.id.tvPaymentStatus)
        val btnPay: Button = view.findViewById(R.id.btnPay)

        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)

        // ✅ Naye views add kiye
        val tvDriverEarning: TextView = view.findViewById(R.id.tvDriverEarning)
        val tvGst: TextView = view.findViewById(R.id.tvGst)
        val tvCommission: TextView = view.findViewById(R.id.tvCommission)
        val tvTotalFare: TextView = view.findViewById(R.id.tvTotalFare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {

        val trip = trips[position]
        val context = holder.itemView.context

        val rideStatus = trip.status ?: "PENDING"
        val paymentStatus = trip.paymentStatus ?: "PENDING"

        holder.tvRideId.text = "Ride ID: ${trip.id ?: ""}"

        val fare = trip.finalFare ?: 0.0

        // ✅ Calculations
        val driverEarning = trip.driverRideEarning ?: 0.0
        val gst = Math.round(fare * 0.18 / 1.18).toInt()   // GST included inside fare
        val zarkitCommission = Math.round(fare - gst - driverEarning).toInt()
        val userTotal = fare.toInt()

        holder.tvPickup.text = "Pickup: ${trip.pickupAddress ?: ""}"
        holder.tvDrop.text = "Drop: ${trip.dropAddress ?: ""}"
        holder.tvFare.text = "Fare: ₹${fare.toInt()}"
        holder.btnPay.text = "Pay ₹$userTotal"
        holder.tvDistance.text = trip.distanceText ?: ""

        holder.tvRideStatus.text = "Status: $rideStatus"
        holder.tvPaymentStatus.text = "Payment: $paymentStatus"
        holder.tvRideId.text = "Ride ID: ${trip.id ?: ""}"

        // ✅ Breakdown bind
        holder.tvDriverEarning.text = "₹${driverEarning.toInt()}"
        holder.tvGst.text = "₹$gst"
        holder.tvCommission.text = "₹$zarkitCommission"
        holder.tvTotalFare.text = "₹$userTotal"

        trip.createdAt?.let {

            try {

                val input = LocalDateTime.parse(it)

                val outputFormatter =
                    DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

                holder.tvDateTime.text =
                    input.format(outputFormatter)

            } catch (e: Exception) {

                holder.tvDateTime.text = it
            }
        }

        when (rideStatus.uppercase()) {
            "COMPLETED" -> {
                holder.tvRideStatus.setTextColor(
                    context.getColor(android.R.color.holo_green_dark)
                )
            }

            "CANCELLED" -> {
                holder.tvRideStatus.setTextColor(
                    context.getColor(android.R.color.holo_red_dark)
                )
            }

            "STARTED", "ACCEPTED" -> {
                holder.tvRideStatus.setTextColor(
                    context.getColor(android.R.color.holo_blue_dark)
                )
            }

            "PENDING" -> {
                holder.tvRideStatus.setTextColor(
                    context.getColor(android.R.color.darker_gray)
                )
            }

            else -> {
                holder.tvRideStatus.setTextColor(
                    context.getColor(android.R.color.darker_gray)
                )
            }
        }

        when (paymentStatus.uppercase()) {

            "PENDING" -> {
                holder.tvPaymentStatus.setTextColor(
                    context.getColor(android.R.color.holo_red_dark)
                )

                holder.btnPay.visibility =
                    if (rideStatus.uppercase() == "CANCELLED") {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
            }

            "ONLINE_PAY" -> {
                holder.tvPaymentStatus.setTextColor(
                    context.getColor(android.R.color.holo_blue_dark)
                )
                holder.btnPay.visibility = View.GONE
            }

            "CASH" -> {
                holder.tvPaymentStatus.setTextColor(
                    context.getColor(android.R.color.holo_green_dark)
                )
                holder.btnPay.visibility = View.GONE
            }

            else -> {
                holder.tvPaymentStatus.setTextColor(
                    context.getColor(android.R.color.darker_gray)
                )
                holder.btnPay.visibility = View.GONE
            }
        }

        if (rideStatus.uppercase() == "CANCELLED") {
            holder.btnPay.visibility = View.GONE
        }

        holder.btnPay.setOnClickListener {
            trip.id?.let {
                val intent = Intent(context, CollectPaymentActivity::class.java)
                intent.putExtra("rideId", it)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = trips.size
}