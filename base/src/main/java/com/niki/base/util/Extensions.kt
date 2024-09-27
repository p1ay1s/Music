package com.niki.base.util

import android.widget.Adapter
import com.google.gson.Gson
import okhttp3.ResponseBody


val Adapter.TAG
    get() = this::class.simpleName!!

inline fun <reified T> convertErrorBody(responseErrorBody: ResponseBody?): T? {
    runCatching {
        return Gson().fromJson(responseErrorBody!!.string(), T::class.java)
    }
    return null
}