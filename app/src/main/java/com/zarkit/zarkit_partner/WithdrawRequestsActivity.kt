package com.zarkit.zarkit_partner

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zarkit.zarkit_partner.api.ApiClient
import com.zarkit.zarkit_partner.api.WithdrawRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WithdrawRequestsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WithdrawRequestsAdapter
    private val withdrawRequests = mutableListOf<WithdrawRequest>()

    private lateinit var txtTitle: TextView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_withdraw_requests)

        //  EDGE TO EDGE APPLY
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

        txtTitle = findViewById(R.id.txtTitle)
        btnBack = findViewById(R.id.btnBack)
        recyclerView = findViewById(R.id.recyclerViewWithdrawRequests)

        txtTitle.text = "All Withdraw Requests"

        btnBack.setOnClickListener { onBackPressed() }

        adapter = WithdrawRequestsAdapter(withdrawRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchWithdrawRequests()
    }

    private fun fetchWithdrawRequests() {
        val token = LocalStorage.getToken(this) ?: return
        val driverId = LocalStorage.getUserId(this)

        ApiClient.api.getAllWithdrawRequests("Bearer $token", driverId)
            .enqueue(object : Callback<List<WithdrawRequest>> {
                override fun onResponse(
                    call: Call<List<WithdrawRequest>>,
                    response: Response<List<WithdrawRequest>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        withdrawRequests.clear()
                        withdrawRequests.addAll(response.body()!!)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@WithdrawRequestsActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<WithdrawRequest>>, t: Throwable) {
                    Toast.makeText(this@WithdrawRequestsActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
