package com.example.ocx_1001_driverapp.uploadsscreen

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ocx_1001_driverapp.R
import java.io.File

class CaptureDLActivity : ComponentActivity() {

    private lateinit var imageCapture: ImageCapture
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_dl)

        // 🔥 Request CAMERA permission first
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) startCamera()
            else finish()
        }

        permissionLauncher.launch(Manifest.permission.CAMERA)

        // CAPTURE BUTTON
        findViewById<ImageView>(R.id.captureButton).setOnClickListener {
            captureDL()
        }

        // BACK BUTTON
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            val previewView = findViewById<androidx.camera.view.PreviewView>(R.id.previewView)
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureDL() {
        val file = File(cacheDir, "DL_${System.currentTimeMillis()}.jpg")
        imageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    setResult(RESULT_OK)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }
}
