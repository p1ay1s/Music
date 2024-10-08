package com.niki.utils

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.p1ay1s.dev.base.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

// 摘自招新系统，略有修改

fun sendBroadcast(msg: String) = appContext?.let {
    LocalBroadcastManager.getInstance(it).sendBroadcast(Intent(msg))
}

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