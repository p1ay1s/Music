package com.niki.music

import android.content.Context
import com.p1ay1s.base.App
import com.p1ay1s.base.CrashActivity
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.log.Logger
import com.p1ay1s.base.log.Logger.startLogger
import com.p1ay1s.base.log.VERBOSE
import com.p1ay1s.util.ServiceBuilder

class MyApp : App() {

    override fun whenOnCreate(context: Context) {
        ServiceBuilder.setTimeout(4L)

//        onNetworkConnectChangedCallback = {
//            IPSetter.setIp()
//            appBaseUrl = "http://$appIpAddress:3000/"
//            logI("App", appBaseUrl)
        appBaseUrl = "https://1330425681-0e8c50d54t.ap-guangzhou.tencentscf.com/"
//        }

        Logger.crashActivity = CrashApp::class.java
        startLogger(context, VERBOSE)
//        onNetworkConnectChangedCallback()
    }
}

class CrashApp : CrashActivity()
