package com.example.ocx_1001_driverapp

import android.content.Context

object LocalStorage {

    private const val PREF_NAME = "driver_app_pref"

    private const val KEY_PHONE = "mobile"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_ROLE = "user_role"
    private const val KEY_FCM = "fcm_token"
    private const val KEY_USER_ID = "user_id"

    // -------------------------
    // USER ID
    // -------------------------

    fun saveUserId(context: Context, userId: Long) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): Long {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getLong(KEY_USER_ID, 0)
    }

    // -------------------------
    // FCM TOKEN
    // -------------------------

    fun saveFcmToken(context: Context, token: String) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_FCM, token).apply()
    }

    fun getFcmToken(context: Context): String? {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_FCM, null)
    }

    // -------------------------
    // PHONE
    // -------------------------

    fun savePhone(context: Context, phone: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PHONE, phone).apply()
    }

    fun getPhone(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PHONE, null)
    }

    // -------------------------
    // JWT TOKEN
    // -------------------------

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    // -------------------------
    // ROLE
    // -------------------------

    fun saveRole(context: Context, role: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ROLE, role).apply()
    }

    fun getRole(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, null)
    }
}
