package com.example.ocx_1001_driverapp.Fragments

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.ocx_1001_driverapp.LocalStorage
import com.example.ocx_1001_driverapp.R
import com.example.ocx_1001_driverapp.api.ApiClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class Vehicle_formFragment : Fragment() {

    private lateinit var vehicleNumberEt: EditText
    private lateinit var citySpinner: Spinner

    // Vehicle type cards
    private lateinit var card2W: LinearLayout
    private lateinit var card3W: LinearLayout
    private lateinit var cardTruck: LinearLayout

    // Fuel UI
    private lateinit var subTitle: TextView
    private lateinit var subContainer: LinearLayout
    private lateinit var subCard1: LinearLayout
    private lateinit var subCard2: LinearLayout
    private lateinit var subCard3: LinearLayout
    private lateinit var subText1: TextView
    private lateinit var subText2: TextView
    private lateinit var subText3: TextView

    private var rcUri: Uri? = null
    private var isSubmitting = false

    private var selectedCity: String? = null
    private var selectedVehicleType: String? = null
    private var selectedFuelType: String? = null

    // ================= CAMERA =================

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && isAdded) toast("RC captured")
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else toast("Camera permission required")
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_vehicle_form, container, false)

        vehicleNumberEt = view.findViewById(R.id.vehicleNumberEditText)
        citySpinner = view.findViewById(R.id.citySpinner)

        card2W = view.findViewById(R.id.card2W)
        card3W = view.findViewById(R.id.card3W)
        cardTruck = view.findViewById(R.id.cardTruck)

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

        view.findViewById<TextView>(R.id.uploadRC).setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        return view
    }

    // ================= VEHICLE + FUEL =================

    private fun setupVehicleSelection() {

        card2W.setOnClickListener {
            selectVehicle("TWO_WHEELER", listOf("EV", "PETROL"))
            highlightVehicle(card2W)
        }

        card3W.setOnClickListener {
            selectVehicle("THREE_WHEELER", listOf("EV", "PETROL", "CNG"))
            highlightVehicle(card3W)
        }

        cardTruck.setOnClickListener {
            selectVehicle("FOUR_WHEELER", listOf("EV", "PETROL_DIESEL", "CNG"))
            highlightVehicle(cardTruck)
        }
    }

    private fun selectVehicle(type: String, fuels: List<String>) {
        selectedVehicleType = type
        selectedFuelType = null

        subTitle.visibility = View.VISIBLE
        subContainer.visibility = View.VISIBLE

        val cards = listOf(subCard1, subCard2, subCard3)
        val texts = listOf(subText1, subText2, subText3)

        for (i in cards.indices) {
            if (i < fuels.size) {
                cards[i].visibility = View.VISIBLE
                texts[i].text = fuels[i]

                cards[i].setOnClickListener {
                    selectedFuelType = fuels[i]
                    cards.forEach { c -> c.setBackgroundResource(R.drawable.vehicle_card_bg) }
                    cards[i].setBackgroundResource(R.drawable.vehicle_card_selected)
                }
            } else {
                cards[i].visibility = View.GONE
            }
        }
    }

    private fun highlightVehicle(selected: LinearLayout) {
        listOf(card2W, card3W, cardTruck).forEach {
            it.setBackgroundResource(R.drawable.vehicle_card_bg)
        }
        selected.setBackgroundResource(R.drawable.vehicle_card_selected)
    }

    // ================= API =================

    fun submitVehicle(onSuccess: () -> Unit) {

        if (isSubmitting) return

        val vehicleNumber = vehicleNumberEt.text.toString().trim()

        when {
            vehicleNumber.isEmpty() -> {
                vehicleNumberEt.error = "Vehicle number required"
                return
            }
            selectedCity == null -> toast("Select city")
            selectedVehicleType == null -> toast("Select vehicle type")
            selectedFuelType == null -> toast("Select fuel type")
            rcUri == null -> toast("Upload RC image")
            else -> {
                val jwt = LocalStorage.getToken(requireContext())
                if (jwt.isNullOrEmpty()) {
                    toast("Session expired. Login again.")
                    return
                }

                isSubmitting = true

                val rcPart = createImagePart("vehicleImage", rcUri!!)

                ApiClient.api.registerVehicle(
                    authHeader = "Bearer $jwt",
                    vehicleNumber = vehicleNumber,
                    city = selectedCity!!,
                    vehicleType = selectedVehicleType!!,
                    vehicleSubType = selectedFuelType!!,
                    vehicleImage = rcPart
                ).enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        isSubmitting = false
                        if (!isAdded) return

                        if (response.isSuccessful) {
                            toast("Vehicle Registered ✅")
                            onSuccess()
                        } else {
                            toast("Server error: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        isSubmitting = false
                        if (!isAdded) return
                        toast("Network error")
                    }
                })
            }
        }
    }

    // ================= HELPERS =================

    private fun setupCitySpinner() {
        val cities = listOf("Select City", "Jhansi", "Datia", "Orai", "Lalitpur", "Gwalior")
        citySpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cities)

        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedCity = if (position == 0) null else cities[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "vehicle_rc.jpg")
        rcUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        cameraLauncher.launch(rcUri)
    }

    private fun createImagePart(key: String, uri: Uri): MultipartBody.Part {
        val body = requireContext().contentResolver.openInputStream(uri)!!.readBytes()
            .toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(key, "$key.jpg", body)
    }

    private fun toast(msg: String) {
        if (isAdded)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
