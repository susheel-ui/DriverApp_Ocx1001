package com.example.ocx_1001_driverapp.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.ocx_1001_driverapp.R
import com.example.ocx_1001_driverapp.uploadsscreen.CaptureRCActivity

class Vehicle_formFragment : Fragment() {

    private lateinit var vehicleNumberEditText: EditText
    private lateinit var citySpinner: Spinner

    private lateinit var cardTruck: LinearLayout
    private lateinit var card3W: LinearLayout
    private lateinit var card2W: LinearLayout

    private var rcUploaded = false
    private var selectedCity: String? = null
    private var selectedVehicleType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_vehicle_form, container, false)

        // Initialize Inputs
        vehicleNumberEditText = view.findViewById(R.id.vehicleNumberEditText)
        citySpinner = view.findViewById(R.id.citySpinner)

        // Vehicle Cards
        cardTruck = view.findViewById(R.id.cardTruck)
        card3W = view.findViewById(R.id.card3W)
        card2W = view.findViewById(R.id.card2W)

        setupCitySpinner()
        setupVehicleSelection()
        setupRcUpload(view)

        return view
    }

    // ===================== CITY DROPDOWN =====================
    private fun setupCitySpinner() {

        val cities = listOf(
            "Select City",
            "Jhansi",
            "Datia",
            "Orai",
            "Lalitpur",
            "Mahoba",
            "Gwalior",
            "Hamirpur",
            "Banda"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            cities
        )

        citySpinner.adapter = adapter

        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectedCity = if (position == 0) null else cities[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ===================== VEHICLE TYPE SELECT =====================
    private fun setupVehicleSelection() {

        val defaultBG = R.drawable.vehicle_card_bg
        val selectedBG = R.drawable.vehicle_card_selected_blue  // <-- Use your BLUE BG

        fun resetAll() {
            cardTruck.setBackgroundResource(defaultBG)
            card3W.setBackgroundResource(defaultBG)
            card2W.setBackgroundResource(defaultBG)
        }

        cardTruck.setOnClickListener {
            resetAll()
            cardTruck.setBackgroundResource(selectedBG)
            selectedVehicleType = "Truck"
        }

        card3W.setOnClickListener {
            resetAll()
            card3W.setBackgroundResource(selectedBG)
            selectedVehicleType = "3W"
        }

        card2W.setOnClickListener {
            resetAll()
            card2W.setBackgroundResource(selectedBG)
            selectedVehicleType = "2W"
        }
    }

    // ===================== VEHICLE RC UPLOAD =====================
    private fun setupRcUpload(view: View) {
        val uploadRC = view.findViewById<TextView>(R.id.uploadRC)

        uploadRC.setOnClickListener {
            startActivity(Intent(requireContext(), CaptureRCActivity::class.java))
            rcUploaded = true
        }
    }

    // ===================== FORM VALIDATION =====================
    fun isFormValid(): Boolean {

        val vehicleNumber = vehicleNumberEditText.text.toString().trim()

        return vehicleNumber.isNotEmpty() &&
                rcUploaded &&
                selectedCity != null &&
                selectedVehicleType != null
    }
}
