package com.example.ocx_1001_driverapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ocx_1001_driverapp.Fragments.Driver_FormFragment
import com.example.ocx_1001_driverapp.Fragments.Owner_formFragement
import com.example.ocx_1001_driverapp.Fragments.Vehicle_formFragment
import com.example.ocx_1001_driverapp.UtilityClasses.Utility
import com.example.ocx_1001_driverapp.databinding.ActivityResistrationBinding

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResistrationBinding

    private lateinit var ownerFormfragement: Fragment
    private lateinit var vehicleFormfragement: Fragment
    private lateinit var driverFormfragement: Fragment

    private lateinit var currentFragment: Fragment

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init fragments
        ownerFormfragement = Owner_formFragement()
        vehicleFormfragement = Vehicle_formFragment()
        driverFormfragement = Driver_FormFragment()

        currentFragment = ownerFormfragement

        // Load first fragment
        Utility.changeFragment(
            this,
            binding.fragmentContainerView,
            ownerFormfragement
        )

        binding.buttonSubmit.setOnClickListener {

            when (currentFragment) {

                // ================= OWNER =================
                is Owner_formFragement -> {
                    val form = currentFragment as Owner_formFragement

                    form.submitOwnerDetails {

                        // Stepper UI
                        binding.vehicleSelectorTV.setBackgroundResource(
                            R.drawable.stepper_circle_active
                        )
                        binding.ownerSelectorTV.setBackgroundResource(
                            R.drawable.stepper_circle_complete
                        )
                        binding.ownerTV.setTextColor(
                            resources.getColor(R.color.gray)
                        )
                        binding.vehicleTV.setTextColor(
                            resources.getColor(R.color.color_primary)
                        )

                        // Move to Vehicle
                        Utility.changeFragment(
                            this,
                            binding.fragmentContainerView,
                            vehicleFormfragement
                        )
                        currentFragment = vehicleFormfragement
                    }
                }

// ================= VEHICLE =================
                is Vehicle_formFragment -> {

                    val form = currentFragment as Vehicle_formFragment

                    form.submitVehicle {

                        binding.driverSelectorTV.setBackgroundResource(
                            R.drawable.stepper_circle_active
                        )
                        binding.vehicleSelectorTV.setBackgroundResource(
                            R.drawable.stepper_circle_complete
                        )
                        binding.vehicleTV.setTextColor(
                            resources.getColor(R.color.gray)
                        )
                        binding.driverTV.setTextColor(
                            resources.getColor(R.color.color_primary)
                        )

                        Utility.changeFragment(
                            this,
                            binding.fragmentContainerView,
                            driverFormfragement
                        )
                        currentFragment = driverFormfragement

                        binding.buttonSubmit.text = getString(R.string.submit)
                    }
                }

// ================= DRIVER =================
                is Driver_FormFragment -> {

                    val form = currentFragment as Driver_FormFragment

                    form.submitDriver {
                        startActivity(
                            Intent(this, DashboardActivity::class.java)
                        )
                        finish()
                    }
                }

            }
        }
    }
}
