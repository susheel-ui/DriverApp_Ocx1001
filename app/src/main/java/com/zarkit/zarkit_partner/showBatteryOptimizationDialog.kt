package com.zarkit.zarkit_partner

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

fun showBatteryOptimizationDialog(context: Activity) {

    // ✅ Permission already ON hai toh dialog mat dikhao
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) return

    // ✅ 3 din se pehle mat dikhao
    val prefs = context.getSharedPreferences("zarkit_prefs", Context.MODE_PRIVATE)
    val lastShown = prefs.getLong("battery_dialog_last_shown", 0L)
    val threeDays = 3 * 24 * 60 * 60 * 1000L
    if (System.currentTimeMillis() - lastShown < threeDays) return

    // ✅ Timestamp save karo
    prefs.edit().putLong("battery_dialog_last_shown", System.currentTimeMillis()).apply()

    val intent = when {
        isPackageInstalled("com.miui.securitycenter", context) ->
            Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
        isPackageInstalled("com.coloros.safecenter", context) ->
            Intent().apply {
                component = android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.privacypermissionsentry.PermissionTopActivity"
                )
            }
        isPackageInstalled("com.vivo.permissionmanager", context) ->
            Intent().apply {
                component = android.content.ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.PurviewTabActivity"
                )
            }
        else ->
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
    }

    AlertDialog.Builder(context)
        .setTitle("Rides Miss Ho Sakti Hain!")
        .setMessage("Battery optimization band karo taaki har ride request mile.")
        .setPositiveButton("Fix Karo") { _, _ ->
            try { context.startActivity(intent) } catch (e: Exception) { }
        }
        .setNegativeButton("Baad Mein", null)
        .show()
}

fun isPackageInstalled(pkg: String, context: Activity): Boolean {
    return try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: Exception) { false }
}