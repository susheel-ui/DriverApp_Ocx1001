package com.example.ocx_1001_driverapp.Fragments

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.ocx_1001_driverapp.CapturePANActivity
import com.example.ocx_1001_driverapp.CaptureSelfieActivity
import com.example.ocx_1001_driverapp.R
import com.example.ocx_1001_driverapp.uploadsscreen.CaptureAadhaarActivity
import org.w3c.dom.Text
import java.io.File

class Owner_formFragement : Fragment() {

    private var imageUri: Uri? = null
    private var currentUploadType: String = ""

    private var uploadAadhaar = false
    private var uploadPan = false
    private var uploadSelfie = false
    private lateinit var nameInput: EditText


    // CAMERA RESULT
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Toast.makeText(
                    requireContext(),
                    "$currentUploadType photo captured",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "Camera cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    // PERMISSION REQUEST
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera(currentUploadType)
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_owner_form_fragement, container, false)

        // INITIALIZE NAME INPUT ❗❗❗ THIS WAS MISSING
        nameInput = view.findViewById(R.id.editTextName)

        // CLICK LISTENERS
        view.findViewById<TextView>(R.id.uploadAadhaar).setOnClickListener {
            startActivity(Intent(requireContext(), CaptureAadhaarActivity::class.java))
            uploadAadhaar = true
        }

        view.findViewById<TextView>(R.id.uploadPan).setOnClickListener {
            startActivity(Intent(requireContext(), CapturePANActivity::class.java))
            uploadPan = true
        }

        view.findViewById<TextView>(R.id.uploadSelfie).setOnClickListener {
            startActivity(Intent(requireContext(), CaptureSelfieActivity::class.java))
            uploadSelfie = true
        }

        return view
    }

    // ⭐ THIS FUNCTION RETURNS TRUE ONLY IF ALL REQUIRED FIELDS ARE COMPLETED
    fun isFormValid(): Boolean {

        val name = nameInput.text.toString().trim()

        return name.isNotEmpty() &&
                uploadAadhaar &&
                uploadPan &&
                uploadSelfie
    }

    private fun requestCameraPermission(type: String) {
        currentUploadType = type

        // Android 13+ needs READ_MEDIA_IMAGES permission
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.CAMERA
        } else {
            Manifest.permission.CAMERA
        }

        permissionLauncher.launch(permission)
    }

    private fun openCamera(type: String) {
        val file = File(requireContext().cacheDir, "${type}.jpg")

        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        cameraLauncher.launch(imageUri)
    }
}
