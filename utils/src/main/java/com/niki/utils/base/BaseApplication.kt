package com.niki.utils.base

import android.app.Application
import android.content.Context
import com.niki.utils.IPSetter.setBaseUrl

lateinit var appContext: Context
lateinit var baseUrl: String

open class BaseApplication : Application() {

    protected val TAG = this::class.simpleName!!

    override fun onCreate() {
        super.onCreate()
        appContext = this
        Logger.init()
        setBaseUrl()
    }
}