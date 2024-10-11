package com.niki.common.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.p1ay1s.base.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

// 摘自招新系统，略有修改

//fun RecyclerView.setSnapHelper() {
//    if (onFlingListener == null)
//        PagerSnapHelper().attachToRecyclerView(this)
//}
//
//fun RecyclerView.addLineDecoration(context: Context, orientation: Int) {
//    if (itemDecorationCount == 0 && layoutManager != null)
//        addItemDecoration(
//            DividerItemDecoration(
//                context,
//                orientation
//            )
//        )
//}
//
///**
// * @param cannotScrollOrientation 指定的方向
// *  1 -> 无法往下
// *
// * -1 -> 无法往上
// */
//fun RecyclerView.addOnLoadMoreListener_V(cannotScrollOrientation: Int, onLoad: () -> Unit) {
//    addOnScrollListener(object : RecyclerView.OnScrollListener() {
//        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//            super.onScrollStateChanged(recyclerView, newState)
//            if (newState == RecyclerView.SCROLL_STATE_IDLE) // 已停止
//                if (!canScrollVertically(cannotScrollOrientation)) // 在到达末尾
//                    onLoad()
//        }
//    })
//}
//
///**
// * @param cannotScrollOrientation 指定的方向
// *  1 -> 无法往右
// *
// * -1 -> 无法往左
// */
//fun RecyclerView.addOnLoadMoreListener_H(cannotScrollOrientation: Int, onLoad: () -> Unit) {
//    addOnScrollListener(object : RecyclerView.OnScrollListener() {
//        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//            super.onScrollStateChanged(recyclerView, newState)
//            if (newState == RecyclerView.SCROLL_STATE_IDLE) // 已停止
//                if (!canScrollHorizontally(cannotScrollOrientation)) // 在到达末尾
//                    onLoad()
//        }
//    })
//}

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