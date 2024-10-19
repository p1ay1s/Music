package com.niki.music

import android.content.Context
import com.p1ay1s.base.App
import com.p1ay1s.base.CrashActivity
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appIpAddress
import com.p1ay1s.base.log.Logger.startLogger
import com.p1ay1s.base.log.VERBOSE
import com.p1ay1s.util.IPSetter
import com.p1ay1s.util.ServiceBuilder

class MyApp : App() {

    override fun whenOnCreate(context: Context) {
        ServiceBuilder.setTimeout(4L)
        ServiceBuilder.enableLogger = true

//        Logger.crashActivity = CrashApp::class.java // 其实感觉还不如让他直接崩溃

        startLogger(context, VERBOSE)

        IPSetter.setIp()
        appBaseUrl = "http://$appIpAddress:3000/"
    }
}

class CrashApp : CrashActivity()
