package com.niki.common.utils

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.Window
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.niki.common.values.FragmentTag
import com.p1ay1s.base.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

// 摘自招新系统，略有修改

fun getNewTag(index: Int): Int {
    FragmentTag.apply {
        if (index in LISTEN_FRAGMENT..TOP_PLAYLIST_FRAGMENT)
            return LISTEN_FRAGMENT
        if (index in PREVIEW_FRAGMENT..RESULT_FRAGMENT)
            return PREVIEW_FRAGMENT

        return MY_FRAGMENT
    }
}

/**
 * 隐藏状态栏
 * */
@Suppress("DEPRECATION")
fun Window.hideStatusBar() {
    decorView.systemUiVisibility =
        decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

/**
 * 显示状态栏
 * */
@Suppress("DEPRECATION")
fun Window.showStatusBar() {
    decorView.systemUiVisibility =
        decorView.systemUiVisibility and (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                ).inv()
}

/**
 * 隐藏导航栏
 * */
@Suppress("DEPRECATION")
fun Window.hideNavigationBar() {
    decorView.systemUiVisibility =
        decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
}

/**
 * 显示导航栏
 * */
@Suppress("DEPRECATION")
fun Window.showNavigationBar() {
    decorView.systemUiVisibility =
        decorView.systemUiVisibility and (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                ).inv()
}

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