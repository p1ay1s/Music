package com.niki.base.base

import android.app.Application
import android.content.Context
import com.niki.utils.IPSetter.setBaseUrl
import com.niki.utils.toast
import com.niki.utils.webs.ServiceBuilder

lateinit var appContext: Context
lateinit var baseUrl: String

class BaseApplication : Application() {
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