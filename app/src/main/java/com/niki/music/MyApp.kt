package com.niki.music

import android.content.Context
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

class MyApp : App() {

    override fun whenOnCreate(context: Context) {
        ServiceBuilder.setTimeout(4L)
        ServiceBuilder.enableLogger = true
        onNetworkConnectChangedCallback = {
            IPSetter.setIp()
            appBaseUrl = "http://$appIpAddress:3000/"
            logI("App", appBaseUrl)
//        appBaseUrl = "https://1330425681-0e8c50d54t.ap-guangzhou.tencentscf.com/"
        }

        Logger.crashActivity = CrashApp::class.java
        startLogger(context, VERBOSE)
        onNetworkConnectChangedCallback()
    }
}

class CrashApp : CrashActivity()
