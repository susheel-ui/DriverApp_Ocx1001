package com.zarkit.zarkit_partner.Fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.zarkit.zarkit_partner.LocalStorage
import com.zarkit.zarkit_partner.R
import com.zarkit.zarkit_partner.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File

class Driver_FormFragment : Fragment() {

    private lateinit var nameEt: EditText
    private lateinit var phoneEt: EditText
    private lateinit var driveGroup: RadioGroup
    private lateinit var loadingLayout: FrameLayout

    private var dlUri: Uri? = null
    private var isSubmitting = false

    // ================= CAMERA =================

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && isAdded) {
                view?.findViewById<ImageView>(R.id.tickDL)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.uploadDL)?.visibility = View.GONE
                toast("Driving Licence captured ✅")
            }
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
        loadingLayout = view.findViewById(R.id.loadingLayout)

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
        val driveThisVehicle = driveGroup.checkedRadioButtonId == R.id.radio_yes

        when {
            name.isEmpty() -> { nameEt.error = "Driver name required"; return }
            phone.length != 10 -> { phoneEt.error = "Enter valid phone number"; return }
            dlUri == null -> { toast("Upload driving licence"); return }
        }

        val jwt = LocalStorage.getToken(requireContext())
        if (jwt.isNullOrEmpty()) {
            toast("Session expired. Login again.")
            return
        }

        showLoader()
        isSubmitting = true

        // Background thread pe compress + upload
        lifecycleScope.launch(Dispatchers.IO) {

            val licensePart = try {
                createCompressedImagePart("driverLicense", dlUri!!)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoader()
                    isSubmitting = false
                    toast("Image processing failed")
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

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
                        hideLoader()
                        isSubmitting = false
                        if (!isAdded) return

                        if (response.isSuccessful) {
                            toast("Driver registered ✅")
                            LocalStorage.saveIsRegistered(requireContext(), true)
                            onSuccess()
                        } else {
                            toast("Server error: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        hideLoader()
                        isSubmitting = false
                        if (!isAdded) return
                        toast("Network error")
                    }
                })
            }
        }
    }

    // ================= HELPERS =================

    private fun showLoader() {
        loadingLayout.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        loadingLayout.visibility = View.GONE
    }

    private fun openCamera() {
        // Unique filename — conflict nahi hoga
        val file = File(requireContext().cacheDir, "driver_dl_${System.currentTimeMillis()}.jpg")

        dlUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        cameraLauncher.launch(dlUri)
    }

    // Compress + resize — background thread pe call karo
    private fun createCompressedImagePart(key: String, uri: Uri): MultipartBody.Part {

        val inputStream = requireContext().contentResolver.openInputStream(uri)!!
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Max 800px width, aspect ratio maintain
        val maxWidth = 800
        val ratio = maxWidth.toFloat() / original.width
        val newHeight = (original.height * ratio).toInt()

        val resized = Bitmap.createScaledBitmap(original, maxWidth, newHeight, true)
        original.recycle()

        // JPEG 75% quality
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        resized.recycle()

        val body = baos.toByteArray().toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(key, "$key.jpg", body)
    }

    private fun toast(msg: String) {
        if (isAdded)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}