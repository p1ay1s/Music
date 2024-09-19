package com.niki.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Adapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.niki.utils.base.appContext
import com.niki.utils.webs.ServiceBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import kotlin.random.Random


// 摘自招新系统，略有修改

val Activity.TAG
    get() = this::class.simpleName!!
val Fragment.TAG
    get() = this::class.simpleName!!
val ViewModel.TAG
    get() = this::class.simpleName!!
val Adapter.TAG
    get() = this::class.simpleName!!

fun sendBroadcast(msg: String) =
    LocalBroadcastManager.getInstance(appContext).sendBroadcast(Intent(msg))

fun <T> MutableList<T>.updateList(list: List<T>, clean: Boolean = false) {
    val originalSize = this.size
    if (clean) {
        this.clear()
        this.addAll(list)
    } else {
        this.addAll(list)
    }
    val updatedSize = this.size
    Log.d("Utils", "updateList: $originalSize -> $updatedSize")
}

suspend fun <T> takePartOf(list: List<T>, size: Int = 30): List<T> = withContext(Dispatchers.IO) {
    val actualOffset = minOf(size, list.size)
    val random = Random(System.nanoTime())

    val startIndex = random.nextInt(0, list.size - actualOffset + 1)

    val indices = IntArray(actualOffset) { it }
    val newList = list.subList(startIndex, startIndex + actualOffset)
    val result = ArrayList<T>(actualOffset)

    for (i in actualOffset - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val temp = indices[i]
        indices[i] = indices[j]
        indices[j] = temp
    }
    for (i in 0 until actualOffset) {
        result.add(newList[indices[i]])
    }

    return@withContext result
}

suspend fun toastSuspended(msg: String, length: Int = Toast.LENGTH_SHORT) =
    withContext(Dispatchers.Main) {
        toast(msg, length)
    }

fun toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    if (msg.isNotBlank())
        Toast.makeText(appContext, msg, length).show()
}

inline fun <reified T> convertErrorBody(responseErrorBody: ResponseBody?): T? {
    runCatching {
        return Gson().fromJson(responseErrorBody!!.string(), T::class.java)
    }
    //异常不做处理
    return null
}