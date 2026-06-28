package com.zarkit.zarkit_partner

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NetworkMonitor(context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = _isConnected.postValue(true)
        override fun onLost(network: Network) = _isConnected.postValue(false)
    }

    fun start() {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        _isConnected.postValue(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        cm.registerNetworkCallback(NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
    }

    fun stop() = try { cm.unregisterNetworkCallback(callback) } catch (e: Exception) { }
}