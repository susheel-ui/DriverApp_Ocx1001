package com.example.ocx_1001_driverapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

object DashboardHelper {

    fun goOnline(slider: View, parent: RelativeLayout, statusText: TextView, statusDot: ImageView) {
        slider.animate().x(parent.width - slider.width.toFloat()).setDuration(200).start()
        statusText.text = "ONLINE"
        parent.setBackgroundResource(R.drawable.btn_online)
        statusDot.setBackgroundResource(R.drawable.status_dot_green)
        Toast.makeText(slider.context, "You are ONLINE", Toast.LENGTH_SHORT).show()
    }

    fun goOffline(slider: View, parent: RelativeLayout, statusText: TextView, statusDot: ImageView) {
        slider.animate().x(0f).setDuration(200).start()
        statusText.text = "GO Online"
        parent.setBackgroundResource(R.drawable.btn_offline)
        statusDot.setBackgroundResource(R.drawable.status_dot_red)
        Toast.makeText(slider.context, "You are OFFLINE", Toast.LENGTH_SHORT).show()
    }

    fun logout(context: Context) {
        LocalStorage.clearActiveRideId(context)
        LocalStorage.saveToken(context, "")
        LocalStorage.saveUserId(context, 0)

        context.startActivity(Intent(context, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    fun showPopup(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}
