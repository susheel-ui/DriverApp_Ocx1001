package com.zarkit.zarkit_partner.Fragments

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.zarkit.zarkit_partner.LocalStorage
import com.zarkit.zarkit_partner.R
import com.zarkit.zarkit_partner.api.ApiClient
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
    private lateinit var loadingLayout: FrameLayout

    private lateinit var card2W: LinearLayout
    private lateinit var card3W: LinearLayout
    private lateinit var cardTruck: LinearLayout

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
    private var selectedSubType: String? = null

    // ================= CAMERA =================

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && isAdded) {
                toast("RC captured ✅")
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                toast("Camera permission required")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_vehicle_form, container, false)

        // ================= INIT =================

        vehicleNumberEt = view.findViewById(R.id.vehicleNumberEditText)
        citySpinner = view.findViewById(R.id.citySpinner)
        loadingLayout = view.findViewById(R.id.loadingLayout)

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
        setupSubSelection()

        view.findViewById<TextView>(R.id.uploadRC).setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        return view
    }

    // ================= VEHICLE SELECTION =================

    private fun setupVehicleSelection() {

        val vehicleCards = listOf(card2W, card3W, cardTruck)

        fun resetVehicleSelection() {
            vehicleCards.forEach { it.isSelected = false }
            resetSubSelection()
        }

        // ================= 2W =================

        card2W.setOnClickListener {

            resetVehicleSelection()

            card2W.isSelected = true

            selectVehicle(
                type = "TWO_WHEELER",
                title = "Fuel Type",
                subOptions = listOf(
                    "EV",
                    "Petrol/Diesel"
                )
            )
        }

        // ================= 3W =================

        card3W.setOnClickListener {

            resetVehicleSelection()

            card3W.isSelected = true

            selectVehicle(
                type = "THREE_WHEELER",
                title = "Vehicle Type",
                subOptions = listOf(
                    "Auto EV",
                    "CNG",
                    "Petrol/Diesel"
                )
            )
        }

        // ================= TRUCK =================

        cardTruck.setOnClickListener {

            resetVehicleSelection()

            cardTruck.isSelected = true

            selectVehicle(
                type = "FOUR_WHEELER",
                title = "Vehicle Size",
                subOptions = listOf(
                    "8ft",
                    "10ft",
                    "12ft"
                )
            )
        }
    }

    private fun selectVehicle(
        type: String,
        title: String,
        subOptions: List<String>
    ) {

        selectedVehicleType = type
        selectedSubType = null

        subTitle.text = title

        subTitle.visibility = View.VISIBLE
        subContainer.visibility = View.VISIBLE

        val cards = listOf(subCard1, subCard2, subCard3)
        val texts = listOf(subText1, subText2, subText3)

        cards.forEach {
            it.isSelected = false
        }

        for (i in cards.indices) {

            if (i < subOptions.size) {

                cards[i].visibility = View.VISIBLE
                texts[i].text = subOptions[i]

            } else {

                cards[i].visibility = View.GONE
            }
        }
    }

    // ================= SUB TYPE SELECTION =================

    private fun setupSubSelection() {

        val subCards = listOf(subCard1, subCard2, subCard3)
        val subTexts = listOf(subText1, subText2, subText3)

        subCards.forEachIndexed { index, card ->

            card.setOnClickListener {

                subCards.forEach {
                    it.isSelected = false
                }

                card.isSelected = true

                selectedSubType = subTexts[index].text.toString()
            }
        }
    }

    private fun resetSubSelection() {

        val subCards = listOf(subCard1, subCard2, subCard3)

        subCards.forEach {
            it.isSelected = false
        }

        selectedSubType = null
    }

    // ================= FRONTEND TO BACKEND VALUE =================

    private fun getBackendSubType(frontendText: String): String {

        return when (frontendText) {

            "Auto EV" -> "EV"

            "Petrol/Diesel" -> "PETROL"

            "8ft" -> "EV"

            "10ft" -> "PETROL"

            "12ft" -> "CNG"

            else -> frontendText.uppercase()
        }
    }

    // ================= API SUBMIT =================

    fun submitVehicle(onSuccess: () -> Unit) {

        if (isSubmitting) return

        val vehicleNumber = vehicleNumberEt.text.toString().trim()

        when {

            vehicleNumber.isEmpty() -> {
                vehicleNumberEt.error = "Vehicle number required"
                return
            }

            selectedCity == null -> {
                toast("Select city")
            }

            selectedVehicleType == null -> {
                toast("Select vehicle type")
            }

            selectedSubType == null -> {
                toast("Select subtype")
            }

            rcUri == null -> {
                toast("Upload RC image")
            }

            else -> {

                val jwt = LocalStorage.getToken(requireContext())

                if (jwt.isNullOrEmpty()) {
                    toast("Session expired. Login again.")
                    return
                }

                showLoader()
                isSubmitting = true

                val rcPart = createImagePart("vehicleImage", rcUri!!)

                ApiClient.api.registerVehicle(
                    authHeader = "Bearer $jwt",
                    vehicleNumber = vehicleNumber,
                    city = selectedCity!!,
                    vehicleType = selectedVehicleType!!,

                    // BACKEND OLD VALUE
                    vehicleSubType = getBackendSubType(selectedSubType!!),

                    vehicleImage = rcPart

                ).enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {

                        hideLoader()
                        isSubmitting = false

                        if (!isAdded) return

                        if (response.isSuccessful) {

                            toast("Vehicle Registered ✅")
                            onSuccess()

                        } else {

                            toast("Server error : ${response.code()}")
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseBody>,
                        t: Throwable
                    ) {

                        hideLoader()
                        isSubmitting = false

                        if (!isAdded) return

                        toast("Network error")
                    }
                })
            }
        }
    }

    // ================= CITY =================

    private fun setupCitySpinner() {

        val cities = listOf(
            "Select City",
            "Jhansi",
            "Datia",
            "Orai",
            "Lalitpur",
            "Gwalior"
        )

        citySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            cities
        )

        citySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    selectedCity =
                        if (position == 0) null
                        else cities[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ================= CAMERA =================

    private fun openCamera() {

        val file = File(
            requireContext().cacheDir,
            "vehicle_rc.jpg"
        )

        rcUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        cameraLauncher.launch(rcUri)
    }

    private fun createImagePart(
        key: String,
        uri: Uri
    ): MultipartBody.Part {

        val body = requireContext()
            .contentResolver
            .openInputStream(uri)!!
            .readBytes()
            .toRequestBody("image/jpeg".toMediaType())

        return MultipartBody.Part.createFormData(
            key,
            "$key.jpg",
            body
        )
    }

    // ================= HELPERS =================

    private fun toast(msg: String) {

        if (isAdded) {

            Toast.makeText(
                requireContext(),
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showLoader() {
        loadingLayout.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        loadingLayout.visibility = View.GONE
    }
}