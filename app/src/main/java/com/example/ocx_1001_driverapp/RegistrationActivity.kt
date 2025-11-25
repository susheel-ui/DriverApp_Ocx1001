package com.example.ocx_1001_driverapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ocx_1001_driverapp.Fragments.Driver_FormFragment
import com.example.ocx_1001_driverapp.Fragments.Owner_formFragement
import com.example.ocx_1001_driverapp.Fragments.Vehicle_formFragment
import com.example.ocx_1001_driverapp.UtilityClasses.Utility
import com.example.ocx_1001_driverapp.databinding.ActivityResistrationBinding

class RegistrationActivity : AppCompatActivity() {
    lateinit var binding: ActivityResistrationBinding
    lateinit var ownerFormfragement: Fragment
    lateinit var driverFormfragement: Fragment
    lateinit var vehicleFormfragement: Fragment
    lateinit var currentFragment:Fragment

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResistrationBinding.inflate(layoutInflater)
        ownerFormfragement = Owner_formFragement()
        driverFormfragement = Driver_FormFragment()
        vehicleFormfragement = Vehicle_formFragment()
        currentFragment = ownerFormfragement
//        enableEdgeToEdge()
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.root.id)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        binding.buttonSubmit.setOnClickListener {
            when (currentFragment) {

                is Owner_formFragement -> {
                    val form = currentFragment as Owner_formFragement

//                    if (!form.isFormValid()) {
//                        Toast.makeText(this, "Please complete all Owner details", Toast.LENGTH_SHORT).show()
//                        return@setOnClickListener
//                    }

                    // ✔ Move to Vehicle Form
                    binding.vehicleSelectorTV.setBackgroundResource(R.drawable.stepper_circle_active)
                    binding.ownerSelectorTV.setBackgroundResource(R.drawable.stepper_circle_complete)
                    binding.ownerTV.setTextColor(resources.getColor(R.color.gray))
                    binding.vehicleTV.setTextColor(resources.getColor(R.color.color_primary))

                    Utility.changeFragment(this, binding.fragmentContainerView, vehicleFormfragement)
                    currentFragment = vehicleFormfragement
                }

                is Vehicle_formFragment -> {
                    val form = currentFragment as Vehicle_formFragment

//                    if (!form.isFormValid()) {
//                        Toast.makeText(this, "Please complete all Vehicle details", Toast.LENGTH_SHORT).show()
//                        return@setOnClickListener
//                    }

                    // ✔ Move to Driver Form
                    binding.driverSelectorTV.setBackgroundResource(R.drawable.stepper_circle_active)
                    binding.vehicleSelectorTV.setBackgroundResource(R.drawable.stepper_circle_complete)
                    binding.vehicleTV.setTextColor(resources.getColor(R.color.gray))
                    binding.driverTV.setTextColor(resources.getColor(R.color.color_primary))

                    Utility.changeFragment(this, binding.fragmentContainerView, driverFormfragement)
                    currentFragment = driverFormfragement
                    binding.buttonSubmit.text = resources.getString(R.string.submit)
                }

                is Driver_FormFragment -> {
                    val form = currentFragment as Driver_FormFragment

//                    if (!form.isFormValid()) {
//                        Toast.makeText(this, "Please complete all Driver details", Toast.LENGTH_SHORT).show()
//                        return@setOnClickListener
//                    }

                    startActivity(Intent(this, DashboardActivity::class.java))
                }
            }
        }

        try {
            Utility.changeFragment(this,binding.fragmentContainerView, currentFragment)
        }catch (e:Exception){
            Log.d(TAG, "onCreate: Fragment error : ${e.message}")
        }

    }


}