package com.example.ocx_1001_driverapp.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ocx_1001_driverapp.R
import com.example.ocx_1001_driverapp.uploadsscreen.CaptureDLActivity

class Driver_FormFragment : Fragment() {

    private var dlUploaded = false
    private lateinit var uploadDL: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_driver__form, container, false)

        uploadDL = view.findViewById(R.id.uploadDL)

        uploadDL.setOnClickListener {
            startActivity(Intent(requireContext(), CaptureDLActivity::class.java))
            dlUploaded = true        // mark as uploaded
        }

        return view
    }

    // Used for Step Validation in RegistrationActivity
    fun isFormValid(): Boolean {
        return dlUploaded       // only DL required
    }
}
