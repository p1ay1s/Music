package com.niki.common.utils

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.FragmentTag
import com.p1ay1s.base.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

fun <T> shuffle(list: MutableList<T>) {
    // 从列表末尾开始
    for (i in list.size - 1 downTo 1) {
        // 得到一个随机索引 `j` 使得 `0 <= j <= i`
        val j = Random.nextInt(i + 1)

        // 将列表中第 i 个位置的元素与第 j 个位置的元素交换
        val temp = list[i]
        list[i] = list[j]
        list[j] = temp
    }
}

fun getNewTag(index: Int): Int {
    FragmentTag.apply {
        if (index in LISTEN_FRAGMENT..TOP_PLAYLIST_FRAGMENT)
            return LISTEN_FRAGMENT
        if (index == RESULT_FRAGMENT)
            return RESULT_FRAGMENT

        return MY_FRAGMENT
    }
}

data class Point(val x: Float, val y: Float)

/**
 * 求直线ab、cd的交点坐标
 */
fun getIntersectionPoint(a: Point, b: Point, c: Point, d: Point): Point? {
    val denominator = (a.x - b.x) * (c.y - d.y) - (a.y - b.y) * (c.x - d.x)

    if (denominator == 0.0F) {
        return null // 直线平行或重合,无交点
    }

    val t = ((a.x - c.x) * (c.y - d.y) - (a.y - c.y) * (c.x - d.x)) / denominator

    val intersectionX = a.x + t * (b.x - a.x)
    val intersectionY = a.y + t * (b.y - a.y)

    return Point(intersectionX, intersectionY)
}

fun TextView.setSongDetails(song: Song) {
    setSingerName(song)
    val singerName = text.toString()
    val builder = StringBuilder()
    builder.apply {
        if (song.al.name.isNotBlank()) {
            append(" • ")
            append(song.al.name)
        }
    }
    text = singerName + builder.toString()
}

/**
 * 效果 : 'a, b, c & d'
 */
fun TextView.setSingerName(song: Song) {
    visibility = View.VISIBLE
    val builder = StringBuilder()
    builder.apply {
        for (artist in song.ar) {
            if (artist.name.isNotBlank()) {
                append(artist.name)
            } else {
                continue
            }
            val index = song.ar.indexOf(artist)
            when (index) { // 效果: a, b, c & d
                song.ar.size - 1 -> {} // the last
                song.ar.size - 2 -> append(" & ")
                else -> append(", ")
            }
        }
    }
    text = builder.toString()
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