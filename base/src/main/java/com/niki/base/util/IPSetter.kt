package com.niki.base.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import com.niki.base.appContext
import com.niki.base.baseUrl
import com.niki.base.logD
import com.niki.base.logE
import java.net.Inet4Address
import java.net.NetworkInterface

object IPSetter {
    private val TAG = this::class.simpleName!!

    init {
        val filter = IntentFilter()
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        appContext.registerReceiver(NetworkConnectChangedReceiver(), filter)
    }

    fun getIp(): String {
        var ip = "0.0.0.0"

        val wifiManager: WifiManager =
            appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager: ConnectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            if (wifiManager.isWifiEnabled) {
                val network = connectivityManager.activeNetwork ?: return ip
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ip
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    val wifiInfo = wifiManager.connectionInfo
                    val ipAddress = wifiInfo.ipAddress

                    ip = (ipAddress and 0xFF).toString() + "." +
                            ((ipAddress shr 8) and 0xFF) + "." +
                            ((ipAddress shr 16) and 0xFF) + "." +
                            (ipAddress shr 24 and 0xFF)
                }
            } else {
                NetworkInterface.getNetworkInterfaces().toList().forEach { networkInterface ->
                    networkInterface.inetAddresses.toList().forEach { inetAddress ->
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            inetAddress.hostAddress?.let { ip = it }
                        }
                    }
                }
            }
            logD(TAG, ip)
        } catch (_: Exception) {
            logE(TAG, "failed")
        }
        return ip
    }

    fun setBaseUrl() {
        baseUrl = "http://${getIp()}:3000/"
    }

    class NetworkConnectChangedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                with(intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)) {
                    if (this != null && isConnected) setBaseUrl()
                }
            }
        }
    }
}