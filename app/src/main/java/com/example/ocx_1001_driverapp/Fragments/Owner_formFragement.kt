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
    private lateinit var loadingLayout: View

    private var aadhaarUri: Uri? = null
    private var aadhaarBackUri: Uri? = null
    private var panUri: Uri? = null
    private var selfieUri: Uri? = null

    private var currentType = ""
    private var isSubmitting = false

    // ================= CAMERA =================
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success && isAdded) {

                when (currentType) {

                    "aadhaar" -> {
                        view?.findViewById<View>(R.id.tickAadhaar)?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.uploadAadhaar)?.visibility = View.GONE
                        toast("Aadhaar Front captured")
                    }

                    "aadhaar_back" -> {
                        view?.findViewById<View>(R.id.tickAadhaarBack)?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.uploadAadhaarBack)?.visibility = View.GONE
                        toast("Aadhaar Back captured")
                    }

                    "pan" -> {
                        view?.findViewById<View>(R.id.tickPan)?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.uploadPan)?.visibility = View.GONE
                        toast("PAN captured")
                    }

                    "selfie" -> {
                        view?.findViewById<View>(R.id.tickSelfie)?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.uploadSelfie)?.visibility = View.GONE
                        toast("Selfie captured")
                    }
                }
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera(currentType)
            else toast("Camera permission required")
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_owner_form_fragement, container, false)

        nameInput = view.findViewById(R.id.editTextName)
        loadingLayout = view.findViewById(R.id.loadingLayout)

        // Aadhaar Front
        view.findViewById<TextView>(R.id.uploadAadhaar).setOnClickListener {
            requestCamera("aadhaar")
        }

        // Aadhaar Back
        view.findViewById<TextView>(R.id.uploadAadhaarBack).setOnClickListener {
            requestCamera("aadhaar_back")
        }

        // PAN
        view.findViewById<TextView>(R.id.uploadPan).setOnClickListener {
            requestCamera("pan")
        }

        // Selfie
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
            "aadhaar_back" -> aadhaarBackUri = uri
            "pan" -> panUri = uri
            "selfie" -> selfieUri = uri
        }

        cameraLauncher.launch(uri)
    }

    // ================= API =================
    fun submitOwnerDetails(onSuccess: () -> Unit) {

        if (isSubmitting) return

        val name = nameInput.text.toString().trim()

        // ✅ VALIDATION
        when {
            name.isEmpty() -> {
                nameInput.error = "Name is required"
                nameInput.requestFocus()
                return
            }
            aadhaarUri == null -> {
                toast("Upload Aadhaar Front")
                return
            }
            aadhaarBackUri == null -> {
                toast("Upload Aadhaar Back")
                return
            }
            panUri == null -> {
                toast("Upload PAN card")
                return
            }
            selfieUri == null -> {
                toast("Upload Selfie")
                return
            }
        }

        val jwt = LocalStorage.getToken(requireContext())
        if (jwt.isNullOrEmpty()) {
            toast("Session expired. Login again.")
            return
        }

        isSubmitting = true
        showLoader()

        val photo1 = createPart("photo1", aadhaarUri!!)
        val photo2 = createPart("photo2", panUri!!)
        val photo3 = createPart("photo3", selfieUri!!)
        val photo4 = createPart("photo4", aadhaarBackUri!!)

        ApiClient.api.registerDriver(
            authHeader = "Bearer $jwt",
            name = name,
            photo1 = photo1,
            photo2 = photo2,
            photo3 = photo3
            // photo4 add if backend accepts
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                isSubmitting = false
                if (!isAdded) return

                hideLoader()

                if (response.isSuccessful) {
                    toast("Owner details saved")
                    onSuccess()
                } else {
                    toast("Server error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                isSubmitting = false
                if (!isAdded) return

                hideLoader()
                toast("Network error. Try again")
            }
        })
    }

    // ================= FILE PART =================
    private fun createPart(key: String, uri: Uri): MultipartBody.Part {

        val body = requireContext().contentResolver.openInputStream(uri)?.use {
            it.readBytes().toRequestBody("image/jpeg".toMediaType())
        } ?: throw IllegalStateException("Cannot read file")

        return MultipartBody.Part.createFormData(key, "$key.jpg", body)
    }

    // ================= LOADER =================
    private fun showLoader() {
        loadingLayout.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        loadingLayout.visibility = View.GONE
    }

    private fun toast(msg: String) {
        if (isAdded)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
