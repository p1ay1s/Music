package com.niki.base.util

import android.app.Activity
import android.widget.Adapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

val Activity.TAG
    get() = this::class.simpleName!!
val Fragment.TAG
    get() = this::class.simpleName!!
val ViewModel.TAG
    get() = this::class.simpleName!!
val Adapter.TAG
    get() = this::class.simpleName!!

suspend fun toastSuspended(msg: String, length: Int = Toast.LENGTH_SHORT) =
    withContext(Dispatchers.Main) {
        toast(msg, length)
    }

fun toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    if (msg.isNotBlank())
        Toast.makeText(com.niki.base.appContext, msg, length).show()
}

inline fun <reified T> convertErrorBody(responseErrorBody: ResponseBody?): T? {
    runCatching {
        return Gson().fromJson(responseErrorBody!!.string(), T::class.java)
    }
    return null
}