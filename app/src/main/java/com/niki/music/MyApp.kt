package com.niki.music

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.p1ay1s.base.App
import com.p1ay1s.base.CrashActivity
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appIpAddress
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.extension.withPermission
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
        }

        Logger.crashActivity = CrashApp::class.java
        startLogger(context, VERBOSE)
        onNetworkConnectChangedCallback()
    }
}

class CrashApp : CrashActivity()
