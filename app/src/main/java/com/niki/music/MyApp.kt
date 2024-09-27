package com.niki.music

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.p1ay1s.dev.base.App
import com.p1ay1s.dev.base.appBaseUrl
import com.p1ay1s.dev.base.appIpAddress
import com.p1ay1s.dev.log.INFO
import com.p1ay1s.dev.log.Logger
import com.p1ay1s.dev.util.IPSetter

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MyApp : App() {

    override fun whenOnCreate(appContext: Context) {
        Logger.run {
            crashActivity = CrashApp::class.java
            start(this@MyApp, appContext, INFO)
        }
        Thread {
            while (appIpAddress == "") {
                IPSetter.setIp()
            }
            appBaseUrl = "http://$appIpAddress:3000/"
        }.start()
    }
}