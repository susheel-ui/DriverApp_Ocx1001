package com.example.ocx_1001_driverapp

import android.content.Context

object LocalStorage {

    private const val PREF_NAME = "driver_app_pref"
    private const val KEY_PHONE = "mobile"

    fun savePhone(context: Context, phone: String) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_PHONE, phone).apply()
    }

    fun getPhone(context: Context): String? {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_PHONE, null)
    }
}
