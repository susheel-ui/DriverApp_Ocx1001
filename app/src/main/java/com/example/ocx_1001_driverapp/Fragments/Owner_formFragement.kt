package com.example.ocx_1001_driverapp.Fragments

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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

class Owner_formFragement : Fragment() {

    private lateinit var nameInput: EditText

    private var aadhaarUri: Uri? = null
    private var panUri: Uri? = null
    private var selfieUri: Uri? = null

    private var currentType = ""
    private var isSubmitting = false

    // ================= CAMERA =================
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && isAdded) {
                toast("${currentType} captured")
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera(currentType)
            else toast("Camera permission denied")
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_owner_form_fragement, container, false)

        nameInput = view.findViewById(R.id.editTextName)

        view.findViewById<TextView>(R.id.uploadAadhaar).setOnClickListener {
            requestCamera("aadhaar")
        }

        view.findViewById<TextView>(R.id.uploadPan).setOnClickListener {
            requestCamera("pan")
        }

        view.findViewById<TextView>(R.id.uploadSelfie).setOnClickListener {
            requestCamera("selfie")
        }

        return view
    }

    private fun requestCamera(type: String) {
        currentType = type
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera(type: String) {
        val file = File(requireContext().cacheDir, "$type.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        when (type) {
            "aadhaar" -> aadhaarUri = uri
            "pan" -> panUri = uri
            "selfie" -> selfieUri = uri
        }

        cameraLauncher.launch(uri)
    }

    // ================= API (TEST MODE) =================
    fun submitOwnerDetails(onSuccess: () -> Unit) {

        if (isSubmitting) return
        isSubmitting = true

        // 👉 For testing, allow empty name
        val name = nameInput.text.toString().trim().ifEmpty { "TEST USER" }

        val jwt = LocalStorage.getToken(requireContext())

        // 👉 If token missing, skip API and go next
        if (jwt.isNullOrEmpty()) {
            isSubmitting = false
            toast("TEST MODE: Skipping API")
            onSuccess()
            return
        }

        // 👉 If images missing, skip API
        if (aadhaarUri == null || panUri == null || selfieUri == null) {
            isSubmitting = false
            toast("TEST MODE: Skipping upload")
            onSuccess()
            return
        }

        // 🔵 Normal API call (only if everything exists)
        ApiClient.api.registerDriver(
            authHeader = "Bearer $jwt",
            name = name,
            photo1 = createPart("photo1", aadhaarUri!!),
            photo2 = createPart("photo2", panUri!!),
            photo3 = createPart("photo3", selfieUri!!)
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                isSubmitting = false
                if (!isAdded) return

                toast("TEST MODE: API done")
                onSuccess()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                isSubmitting = false
                if (!isAdded) return

                toast("TEST MODE: API failed, moving next")
                onSuccess()
            }
        })
    }

    // ================= FILE PART =================
    private fun createPart(key: String, uri: Uri): MultipartBody.Part {

        val inputStream = requireContext()
            .contentResolver
            .openInputStream(uri)
            ?: throw IllegalStateException("Cannot open file")

        val bytes = inputStream.readBytes()
        inputStream.close()

        val body = bytes.toRequestBody("image/*".toMediaType())
        return MultipartBody.Part.createFormData(key, "$key.jpg", body)
    }

    private fun toast(msg: String) {
        if (isAdded)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}










//package com.example.ocx_1001_driverapp.Fragments
//
//import android.Manifest
//import android.net.Uri
//import android.os.Bundle
//import android.view.*
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.FileProvider
//import androidx.fragment.app.Fragment
//import com.example.ocx_1001_driverapp.LocalStorage
//import com.example.ocx_1001_driverapp.R
//import com.example.ocx_1001_driverapp.api.ApiClient
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.MultipartBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import okhttp3.ResponseBody
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.io.File
//
//class Owner_formFragement : Fragment() {
//
//    private lateinit var nameInput: EditText
//
//    private var aadhaarUri: Uri? = null
//    private var panUri: Uri? = null
//    private var selfieUri: Uri? = null
//
//    private var currentType = ""
//    private var isSubmitting = false   // ✅ FIX: prevent multiple clicks
//
//    // ================= CAMERA =================
//    private val cameraLauncher =
//        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//            if (success && isAdded) {
//                toast("${currentType.capitalize()} captured")
//            }
//        }
//
//    private val permissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) openCamera(currentType)
//            else toast("Camera permission required")
//        }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//
//        val view = inflater.inflate(R.layout.fragment_owner_form_fragement, container, false)
//
//        nameInput = view.findViewById(R.id.editTextName)
//
//        view.findViewById<TextView>(R.id.uploadAadhaar).setOnClickListener {
//            requestCamera("aadhaar")
//        }
//
//        view.findViewById<TextView>(R.id.uploadPan).setOnClickListener {
//            requestCamera("pan")
//        }
//
//        view.findViewById<TextView>(R.id.uploadSelfie).setOnClickListener {
//            requestCamera("selfie")
//        }
//
//        return view
//    }
//
//    private fun requestCamera(type: String) {
//        currentType = type
//        permissionLauncher.launch(Manifest.permission.CAMERA)
//    }
//
//    private fun openCamera(type: String) {
//        val file = File(requireContext().cacheDir, "$type.jpg")
//        val uri = FileProvider.getUriForFile(
//            requireContext(),
//            "${requireContext().packageName}.provider",
//            file
//        )
//
//        when (type) {
//            "aadhaar" -> aadhaarUri = uri
//            "pan" -> panUri = uri
//            "selfie" -> selfieUri = uri
//        }
//
//        cameraLauncher.launch(uri)
//    }
//
//    // ================= API =================
//    fun submitOwnerDetails(onSuccess: () -> Unit) {
//
//        if (isSubmitting) return   // ✅ FIX
//
//        val name = nameInput.text.toString().trim()
//
//        // ✅ STRONG VALIDATION
//        when {
//            name.isEmpty() -> {
//                nameInput.error = "Name is required"
//                nameInput.requestFocus()
//                return
//            }
//            aadhaarUri == null -> {
//                toast("Upload Aadhaar card")
//                return
//            }
//            panUri == null -> {
//                toast("Upload PAN card")
//                return
//            }
//            selfieUri == null -> {
//                toast("Upload Selfie")
//                return
//            }
//        }
//
//        val jwt = LocalStorage.getToken(requireContext())
//        if (jwt.isNullOrEmpty()) {
//            toast("Session expired. Login again.")
//            return
//        }
//
//        isSubmitting = true   // ✅ START SUBMIT
//
//        val photo1 = createPart("photo1", aadhaarUri!!)
//        val photo2 = createPart("photo2", panUri!!)
//        val photo3 = createPart("photo3", selfieUri!!)
//
//        ApiClient.api.registerDriver(
//            authHeader = "Bearer $jwt",
//            name = name,
//            photo1 = photo1,
//            photo2 = photo2,
//            photo3 = photo3
//        ).enqueue(object : Callback<ResponseBody> {
//
//            override fun onResponse(
//                call: Call<ResponseBody>,
//                response: Response<ResponseBody>
//            ) {
//                isSubmitting = false
//
//                if (!isAdded) return
//
//                if (response.isSuccessful) {
//                    toast("Owner details saved")
//                    onSuccess()
//                } else {
//                    toast("Server error: ${response.code()}")
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                isSubmitting = false
//                if (!isAdded) return
//                toast("Network error. Try again")
//            }
//        })
//    }
//
//    // ================= FILE PART =================
//    private fun createPart(key: String, uri: Uri): MultipartBody.Part {
//
//        val inputStream = requireContext()
//            .contentResolver
//            .openInputStream(uri)
//            ?: throw IllegalStateException("Cannot open file")
//
//        val bytes = inputStream.readBytes()
//        inputStream.close()
//
//        val body = bytes.toRequestBody("image/*".toMediaType())
//
//        return MultipartBody.Part.createFormData(key, "$key.jpg", body)
//    }
//
//    private fun toast(msg: String) {
//        if (isAdded)
//            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
//    }
//}