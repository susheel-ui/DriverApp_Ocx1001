package com.zarkit.zarkit_partner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zarkit.zarkit_partner.api.WithdrawRequest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class WithdrawRequestsAdapter(private val requests: List<WithdrawRequest>) :
    RecyclerView.Adapter<WithdrawRequestsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUserName: TextView = itemView.findViewById(R.id.txtUserName)
        val txtUserPhone: TextView = itemView.findViewById(R.id.txtUserPhone)
        val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        val txtAccountNumber: TextView = itemView.findViewById(R.id.txtAccountNumber)
        val txtIfscCode: TextView = itemView.findViewById(R.id.txtIfscCode)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtDate: TextView = itemView.findViewById(R.id.txtRequestTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_withdraw_request, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]

        // User info
        holder.txtUserName.text = request.userName
        holder.txtUserPhone.text = "Phone: ${request.userPhone}"

        // Amount formatting
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        holder.txtAmount.text = formatter.format(request.amount)

        // Bank details (separate rows)
        holder.txtAccountNumber.text = "Account: ${request.accountNumber ?: "-"}"
        holder.txtIfscCode.text = "IFSC: ${request.ifscCode ?: "-"}"

        // Status with color
        holder.txtStatus.text = request.status
        holder.txtStatus.setTextColor(
            when (request.status.uppercase(Locale.getDefault())) {
                "PENDING" -> 0xFFFFA000.toInt() // Amber
                "COMPLETED" -> 0xFF388E3C.toInt() // Green
                "REJECTED" -> 0xFFD32F2F.toInt() // Red
                else -> 0xFF000000.toInt() // Black
            }
        )

        // Date formatting
        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val date = try { sdfInput.parse(request.requestTime) } catch (e: Exception) { null }
        holder.txtDate.text = date?.let { sdfOutput.format(it) } ?: request.requestTime
    }
}
