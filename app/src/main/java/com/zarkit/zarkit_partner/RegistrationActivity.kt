package com.zarkit.zarkit_partner

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.zarkit.zarkit_partner.Fragments.Driver_FormFragment
import com.zarkit.zarkit_partner.Fragments.Owner_formFragement
import com.zarkit.zarkit_partner.Fragments.Vehicle_formFragment
import com.zarkit.zarkit_partner.UtilityClasses.Utility
import com.zarkit.zarkit_partner.databinding.ActivityResistrationBinding

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResistrationBinding

    private lateinit var ownerFormfragement: Fragment
    private lateinit var vehicleFormfragement: Fragment
    private lateinit var driverFormfragement: Fragment

    private lateinit var currentFragment: Fragment

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // ✅ IMPORTANT → use rootLayout (NOT main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // ================= HEADER CLICK LISTENERS =================
        binding.supportButton.setOnClickListener {
            startActivity(
                Intent(this, SupportActivity::class.java)
            )
        }

        binding.backButton.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
        }

        // ================= INIT FRAGMENTS =================
        ownerFormfragement = Owner_formFragement()
        vehicleFormfragement = Vehicle_formFragment()
        driverFormfragement = Driver_FormFragment()

        currentFragment = ownerFormfragement

        // ================= LOAD FIRST FRAGMENT =================
        Utility.changeFragment(
            this,
            binding.fragmentContainerView,
            ownerFormfragement
        )

        // ================= SUBMIT BUTTON =================
        binding.buttonSubmit.setOnClickListener {

            when (currentFragment) {

                // ================= OWNER =================
                is Owner_formFragement -> {
                    val form = currentFragment as Owner_formFragement

                    form.submitOwnerDetails {

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
