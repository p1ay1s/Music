package com.niki.base

import android.app.Application
import android.content.Context
import com.niki.base.log.Logger
import com.niki.base.util.IPSetter.setBaseUrl
import com.niki.base.util.ServiceBuilder
import com.niki.base.util.toast

lateinit var appContext: Context
lateinit var baseUrl: String

open class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Logger.init()
        setBaseUrl()
        ServiceBuilder.ping(baseUrl) { isSuccess ->
            if (!isSuccess)
                toast("服务器连接失败")
        }
    }
}