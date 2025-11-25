package com.example.ocx_1001_driverapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File

class CaptureSelfieActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_selfie)

        previewView = findViewById(R.id.previewView)

        startCamera()

        findViewById<ImageView>(R.id.captureButton).setOnClickListener {
            takeSelfie()
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder().build()

            // FRONT CAMERA
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takeSelfie() {
        val file = File(externalCacheDir, "selfie.jpg")

        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@CaptureSelfieActivity, "Selfie Captured!", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CaptureSelfieActivity, "Error!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
