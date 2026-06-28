package com.zarkit.zarkit_partner

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private var noInternetDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor(this)
    }

    override fun onStart() {
        super.onStart()
        networkMonitor.start()
        networkMonitor.isConnected.observe(this) { connected ->
            if (connected) noInternetDialog?.dismiss()
            else showNoInternet()
        }
    }

    override fun onStop() {
        super.onStop()
        networkMonitor.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        noInternetDialog?.dismiss()
    }

    private fun showNoInternet() {
        if (isFinishing || isDestroyed || noInternetDialog?.isShowing == true) return
        noInternetDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.bottom_sheet_no_internet)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setGravity(Gravity.BOTTOM)
            window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            show()
        }
    }
}