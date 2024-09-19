package com.niki.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import com.niki.utils.base.appContext
import com.niki.utils.base.baseUrl
import com.niki.utils.base.logD
import com.niki.utils.base.logE
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

    fun setBaseUrl() {
        var ip = "0.0.0.0"
        baseUrl = "http://$ip:3000/"

        val wifiManager: WifiManager =
            appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager: ConnectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            if (wifiManager.isWifiEnabled) {
                val network = connectivityManager.activeNetwork ?: return
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return
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
                            ip = inetAddress.hostAddress ?: return
                        }
                    }
                }
            }
        } catch (_: Exception) {
            logE(TAG, "failed to get ip")
            toast("ip信息获取失败")
        } finally {
            baseUrl = "http://$ip:3000/"
            logD(TAG, baseUrl)
        }
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