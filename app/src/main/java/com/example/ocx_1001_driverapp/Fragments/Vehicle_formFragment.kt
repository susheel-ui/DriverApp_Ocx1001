package com.example.ocx_1001_driverapp.Fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.ocx_1001_driverapp.R
import java.io.File

class Vehicle_formFragment : Fragment() {

    private lateinit var vehicleNumberEditText: EditText
    private lateinit var citySpinner: Spinner

    private lateinit var cardTruck: LinearLayout
    private lateinit var card3W: LinearLayout
    private lateinit var card2W: LinearLayout

    // Sub vehicle views
    private lateinit var subTitle: TextView
    private lateinit var subContainer: LinearLayout
    private lateinit var subCard1: LinearLayout
    private lateinit var subCard2: LinearLayout
    private lateinit var subCard3: LinearLayout
    private lateinit var subText1: TextView
    private lateinit var subText2: TextView
    private lateinit var subText3: TextView

    private var rcUploaded = false
    private var rcUri: Uri? = null

    private var selectedCity: String? = null
    private var selectedVehicleType: String? = null
    private var selectedFuelType: String? = null

    // ================= CAMERA =================
    private val rcCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && isAdded) {
                rcUploaded = true
                Toast.makeText(requireContext(), "RC photo captured", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_vehicle_form, container, false)

        vehicleNumberEditText = view.findViewById(R.id.vehicleNumberEditText)
        citySpinner = view.findViewById(R.id.citySpinner)

        cardTruck = view.findViewById(R.id.cardTruck)
        card3W = view.findViewById(R.id.card3W)
        card2W = view.findViewById(R.id.card2W)

        subTitle = view.findViewById(R.id.subVehicleTitle)
        subContainer = view.findViewById(R.id.subVehicleContainer)
        subCard1 = view.findViewById(R.id.subCard1)
        subCard2 = view.findViewById(R.id.subCard2)
        subCard3 = view.findViewById(R.id.subCard3)
        subText1 = view.findViewById(R.id.subText1)
        subText2 = view.findViewById(R.id.subText2)
        subText3 = view.findViewById(R.id.subText3)

        setupCitySpinner()
        setupVehicleSelection()
        setupFuelSelection()
        setupRcUpload(view)

        return view
    }

    // ================= CITY =================
    private fun setupCitySpinner() {
        val cities = listOf("Select City", "Jhansi", "Datia", "Orai", "Lalitpur", "Gwalior")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cities)
        citySpinner.adapter = adapter

        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedCity = if (pos == 0) null else cities[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ================= VEHICLE TYPE =================
    private fun setupVehicleSelection() {

        val defaultBG = R.drawable.vehicle_card_bg
        val selectedBG = R.drawable.vehicle_card_selected_blue

        fun resetMain() {
            cardTruck.setBackgroundResource(defaultBG)
            card3W.setBackgroundResource(defaultBG)
            card2W.setBackgroundResource(defaultBG)
        }

        card2W.setOnClickListener {
            resetMain()
            card2W.setBackgroundResource(selectedBG)
            selectedVehicleType = "2W"
            showFuelOptions("2W")
        }

        card3W.setOnClickListener {
            resetMain()
            card3W.setBackgroundResource(selectedBG)
            selectedVehicleType = "3W"
            showFuelOptions("3W")
        }

        cardTruck.setOnClickListener {
            resetMain()
            cardTruck.setBackgroundResource(selectedBG)
            selectedVehicleType = "4W"
            showFuelOptions("4W")
        }
    }

    // ================= FUEL OPTIONS =================
    private fun showFuelOptions(type: String) {

        subTitle.visibility = View.VISIBLE
        subContainer.visibility = View.VISIBLE
        selectedFuelType = null
        resetFuelBG()

        when (type) {
            "2W" -> {
                subText1.text = "EV"
                subText2.text = "Petrol"
                subCard3.visibility = View.GONE
            }
            "3W" -> {
                subText1.text = "EV"
                subText2.text = "Petrol/Diesel"
                subText3.text = "CNG"
                subCard3.visibility = View.VISIBLE
            }
            "4W" -> {
                subText1.text = "EV"
                subText2.text = "Diesel"
                subText3.text = "CNG"
                subCard3.visibility = View.VISIBLE
            }
        }
    }

    private fun setupFuelSelection() {

        val selectedBG = R.drawable.vehicle_card_selected_blue

        fun select(card: LinearLayout, fuel: String) {
            resetFuelBG()
            card.setBackgroundResource(selectedBG)
            selectedFuelType = fuel
        }

        subCard1.setOnClickListener { select(subCard1, subText1.text.toString()) }
        subCard2.setOnClickListener { select(subCard2, subText2.text.toString()) }
        subCard3.setOnClickListener { select(subCard3, subText3.text.toString()) }
    }

    private fun resetFuelBG() {
        subCard1.setBackgroundResource(R.drawable.vehicle_card_bg)
        subCard2.setBackgroundResource(R.drawable.vehicle_card_bg)
        subCard3.setBackgroundResource(R.drawable.vehicle_card_bg)
    }

    // ================= RC UPLOAD =================
    private fun setupRcUpload(view: View) {
        view.findViewById<TextView>(R.id.uploadRC).setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val file = File(
            requireContext().cacheDir,
            "vehicle_rc_${System.currentTimeMillis()}.jpg"
        )

        rcUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        rcCameraLauncher.launch(rcUri)
    }

    // ================= VALIDATION =================
    fun isFormValid(): Boolean {
        return vehicleNumberEditText.text.toString().isNotEmpty()
                && rcUploaded
                && selectedCity != null
                && selectedVehicleType != null
                && selectedFuelType != null
    }
}
