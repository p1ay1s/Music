package com.niki.music

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.p1ay1s.base.App
import com.p1ay1s.base.CrashActivity
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appIpAddress
import com.p1ay1s.base.log.Logger
import com.p1ay1s.base.log.Logger.startLogger
import com.p1ay1s.base.log.VERBOSE
import com.p1ay1s.base.log.logI
import com.p1ay1s.util.IPSetter
import com.p1ay1s.util.ServiceBuilder
import com.p1ay1s.util.onNetworkConnectChangedCallback

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MyApp : App() {

    override fun whenOnCreate(context: Context) {
        ServiceBuilder.setTimeout(4L)

        onNetworkConnectChangedCallback = {
            IPSetter.setIp()
            appBaseUrl = "http://$appIpAddress:3000/"
            logI("App", appBaseUrl)
        }

        Logger.crashActivity = CrashApp::class.java
        startLogger(context, VERBOSE)
        onNetworkConnectChangedCallback()
    }
}

class CrashApp : CrashActivity()
