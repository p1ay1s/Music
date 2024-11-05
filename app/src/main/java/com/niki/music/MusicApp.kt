package com.niki.music

import android.content.Context
import com.niki.common.utils.getStringData
import com.niki.common.values.preferenceBaseUrl
import com.p1ay1s.base.App
import com.p1ay1s.base.CrashActivity
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appContext
import com.p1ay1s.base.appIpAddress
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.log.Logger
import com.p1ay1s.base.log.VERBOSE
import com.p1ay1s.util.IPSetter
import com.p1ay1s.util.ServiceBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyApp : App() {

    override fun whenOnCreate(context: Context) {
        ServiceBuilder.setTimeout(4L)
        ServiceBuilder.enableLogger = true

        appContext = context
        Logger.setLogLevel(VERBOSE)

        IPSetter.setIp()
    }
}

class CrashApp : CrashActivity()
