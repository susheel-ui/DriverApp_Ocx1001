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

class Driver_FormFragment : Fragment() {

    private lateinit var nameEt: EditText
    private lateinit var phoneEt: EditText
    private lateinit var driveGroup: RadioGroup

    private var dlUri: Uri? = null
    private var isSubmitting = false

    // ================= CAMERA =================

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && isAdded) toast("Driving Licence captured")
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

        val view = inflater.inflate(R.layout.fragment_driver_form, container, false)

        nameEt = view.findViewById(R.id.driverNameEt)
        phoneEt = view.findViewById(R.id.driverPhoneEt)
        driveGroup = view.findViewById(R.id.driveVehicleGroup)

        view.findViewById<TextView>(R.id.uploadDL).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        return view
    }

    // ================= SUBMIT =================

    fun submitDriver(onSuccess: () -> Unit) {

        if (isSubmitting) return

        val name = nameEt.text.toString().trim()
        val phone = phoneEt.text.toString().trim()
        val driveThisVehicle =
            driveGroup.checkedRadioButtonId == R.id.radio_yes

        when {
            name.isEmpty() -> {
                nameEt.error = "Driver name required"
                return
            }
            phone.length != 10 -> {
                phoneEt.error = "Enter valid phone number"
                return
            }
            dlUri == null -> {
                toast("Upload driving licence")
                return
            }
        }

        val jwt = LocalStorage.getToken(requireContext())
        if (jwt.isNullOrEmpty()) {
            toast("Session expired. Login again.")
            return
        }

        isSubmitting = true

        val licensePart = createImagePart("driverLicense", dlUri!!)

        ApiClient.api.registerAssignedDriver(
            authHeader = "Bearer $jwt",
            driveThisVehicle = driveThisVehicle,
            driverName = name,
            driverPhone = phone,
            driverLicense = licensePart
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                isSubmitting = false
                if (!isAdded) return

                if (response.isSuccessful) {
                    toast("Driver registered ✅")
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

    // ================= HELPERS =================

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "driver_dl.jpg")
        dlUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        cameraLauncher.launch(dlUri)
    }

    private fun createImagePart(key: String, uri: Uri): MultipartBody.Part {
        val body = requireContext().contentResolver.openInputStream(uri)!!
            .readBytes()
            .toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(key, "$key.jpg", body)
    }

    private fun toast(msg: String) {
        if (isAdded)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
