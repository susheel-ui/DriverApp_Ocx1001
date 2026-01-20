package com.example.ocx_1001_driverapp

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ocx_1001_driverapp.databinding.ActivityQrPaymentBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class QrPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Razorpay TEST payment link
        val paymentLink = "https://rzp.io/l/testRidePayment"

        val qrBitmap = generateQr(paymentLink)
        binding.imgQr.setImageBitmap(qrBitmap)
    }

    private fun generateQr(text: String): Bitmap {
        val size = 800
        val bitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                )
            }
        }
        return bitmap
    }
}
