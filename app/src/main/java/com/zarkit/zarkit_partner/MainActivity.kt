package com.zarkit.zarkit_partner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.common.IntentSenderForResultStarter
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed

class MainActivity : BaseActivity() {

    private val LOCATION_PERMISSION_REQUEST = 101

    // ── NEW: Update variables ──────────────────────────────

    private lateinit var appUpdateManager: AppUpdateManager
    private var currentUpdateType = AppUpdateType.FLEXIBLE

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> showFlexibleUpdateSnackbar()
            InstallStatus.FAILED -> {
                Log.e("UPDATE", "Download failed")
                proceedToPermissions()
            }
            else -> {}
        }
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        when {
            result.resultCode == RESULT_OK
                    && currentUpdateType == AppUpdateType.FLEXIBLE -> {
                proceedToPermissions()
            }
            result.resultCode != RESULT_OK
                    && currentUpdateType == AppUpdateType.IMMEDIATE -> {
                checkForUpdate()
            }
            result.resultCode != RESULT_OK
                    && currentUpdateType == AppUpdateType.FLEXIBLE -> {
                proceedToPermissions()
            }
        }
    }

    private val intentSenderStarter = IntentSenderForResultStarter { intent, _, fillInIntent, flagsMask, flagsValues, _, _ ->
        updateLauncher.launch(
            IntentSenderRequest.Builder(intent)
                .setFillInIntent(fillInIntent)
                .setFlags(flagsValues, flagsMask)
                .build()
        )
    }

    // ── END NEW ───────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //  CREATE NOTIFICATION CHANNEL (Android 8+)
        createNotificationChannel()

        // ── NEW: Update check pehle ────────────────────────
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        appUpdateManager.registerListener(installStateUpdatedListener)
        checkForUpdate()
        // ── END NEW ───────────────────────────────────────
    }

    // ── NEW: Update functions ──────────────────────────────

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                    proceedToPermissions()
                    return@addOnSuccessListener
                }

                val currentVersion = try {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                } catch (e: Exception) { "1.0.0" }

                val newCode = info.availableVersionCode()
                val newVersion = "${newCode / 100}.${(newCode % 100) / 10}.${newCode % 10}"

                Log.d("UPDATE", "Installed: $currentVersion | Available: $newVersion")

                when (UpdateHelper.decideUpdateType(currentVersion, newVersion)) {
                    UpdateHelper.UpdateDecision.IMMEDIATE -> {
                        if (info.isImmediateUpdateAllowed) {
                            currentUpdateType = AppUpdateType.IMMEDIATE
                            appUpdateManager.startUpdateFlowForResult(
                                info,
                                intentSenderStarter,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                                0
                            )
                        } else proceedToPermissions()
                    }
                    UpdateHelper.UpdateDecision.FLEXIBLE -> {
                        if (info.isFlexibleUpdateAllowed) {
                            currentUpdateType = AppUpdateType.FLEXIBLE
                            appUpdateManager.startUpdateFlowForResult(
                                info,
                                intentSenderStarter,
                                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                                0
                            )
                        }
                        proceedToPermissions()
                    }
                    UpdateHelper.UpdateDecision.NONE -> proceedToPermissions()
                }
            }
            .addOnFailureListener {
                Log.e("UPDATE", "Check failed: ${it.message}")
                proceedToPermissions()
            }
    }

    private fun showFlexibleUpdateSnackbar() {
        Snackbar.make(
            findViewById(R.id.main),
            "Naya update ready! Abhi install karo.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("INSTALL KARO") { appUpdateManager.completeUpdate() }
            setActionTextColor(
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_light)
            )
            show()
        }
    }

    private fun proceedToPermissions() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
        } else {
            showBatteryOptimizationDialog(this)
            proceedNext()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::appUpdateManager.isInitialized) return
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                        && currentUpdateType == AppUpdateType.IMMEDIATE -> {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        intentSenderStarter,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        0
                    )
                }
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    showFlexibleUpdateSnackbar()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
    }

    // ── END NEW ───────────────────────────────────────────

    // ── ORIGINAL CODE - Kuch bhi touch nahi kiya ──────────

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            showBatteryOptimizationDialog(this)
            proceedNext()
        } else {
            // Permission denied → app still works, but no live tracking
            proceedNext()
        }
    }

    private fun proceedNext() {
        Handler().post {

            val token = LocalStorage.getToken(this)

            val isRegistered = LocalStorage.isRegistered(this)

            if (token.isNullOrEmpty()) {
                startActivity(Intent(this, LoginActivity::class.java))

            } else if (!isRegistered) {
                startActivity(Intent(this, RegistrationActivity::class.java))

            } else {
                startActivity(Intent(this, DashboardActivity::class.java))
            }

            finish()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "driver_location_channel",
                "Driver Live Location",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}