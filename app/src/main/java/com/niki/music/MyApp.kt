package com.niki.music

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.p1ay1s.dev.base.App
import com.p1ay1s.dev.base.CrashActivity
import com.p1ay1s.dev.base.appBaseUrl
import com.p1ay1s.dev.base.appIpAddress
import com.p1ay1s.dev.log.INFO
import com.p1ay1s.dev.log.Logger
import com.p1ay1s.dev.log.Logger.startLogger
import com.p1ay1s.dev.log.logI
import com.p1ay1s.dev.util.IPSetter
import com.p1ay1s.dev.util.onNetworkConnectChangedCallback

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MyApp : App() {

    override fun whenOnCreate(appContext: Context) {
        onNetworkConnectChangedCallback = {
            IPSetter.setIp()
            appBaseUrl = "http://$appIpAddress:3000/"
            logI("App", appBaseUrl)
        }

        Logger.crashActivity = CrashApp::class.java
        startLogger(appContext, INFO)
        onNetworkConnectChangedCallback()
    }
}

class CrashApp : CrashActivity()
