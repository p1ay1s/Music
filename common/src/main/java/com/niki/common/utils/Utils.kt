package com.niki.common.utils

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.niki.common.repository.dataclasses.album.AlbumDetails
import com.niki.common.repository.dataclasses.playlist.Playlist
import com.niki.common.repository.dataclasses.song.Song
import com.p1ay1s.base.appBaseUrl
import com.p1ay1s.base.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

suspend fun <T> withTimer(timeout: Long, block: suspend CoroutineScope.() -> T) = try {
    withTimeout(timeout, block)
} catch (_: Exception) {
}

inline fun ViewModel.waitForBaseUrl(crossinline callback: () -> Unit) = viewModelScope.launch {
    while (!appBaseUrl.isUrl()) {
        delay(20)
    }
    callback()
}

fun String?.isUrl(): Boolean {
    if (this.isNullOrEmpty()) return false
    return startsWith("https://") || startsWith("http://")
}

fun Activity.restartApplication() {
    packageManager.getLaunchIntentForPackage(packageName)?.run {
        this.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(this)
        Runtime.getRuntime().exit(0)
    }
}

fun Fragment.restartApplication() {
    requireActivity().restartApplication()
}

fun formatDuration(duration: Int): String {
    val m = duration / 1000 / 60
    val s = duration / 1000 % 60

    val minutes: String = m.toString()
    val seconds: String = if (s < 10)
        "0$s"
    else
        s.toString()
    return "$minutes:$seconds"
}

fun Fragment.setViewBelowStatusBar(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        view.setMargins(top = requireActivity().calculateStatusBarHeight())
}

fun View.setSize(size: Int) = setSize(size, size)

fun View.setSize(width: Int? = null, height: Int? = null) = runCatching {
    updateLayoutParams {
        width?.let { this.width = it }
        height?.let { this.height = it }
    }
}

fun View.setHorizontalMargins(margin: Int) {
    setMargins(start = margin, end = margin)
}

fun View.setVerticalMargins(margin: Int) {
    setMargins(top = margin, bottom = margin)
}

fun View.setMargins(margin: Int) {
    setMargins(margin, margin, margin, margin)
}

fun View.setMargins(
    start: Int? = null,
    end: Int? = null,
    top: Int? = null,
    bottom: Int? = null,
) = runCatching {
    val lp = layoutParams
    (lp as? ViewGroup.MarginLayoutParams)?.run {
        start?.let { leftMargin = it }
        end?.let { rightMargin = it }
        top?.let { topMargin = it }
        bottom?.let { bottomMargin = it }
    } ?: return@runCatching
    layoutParams = lp
    requestLayout()
}

@RequiresApi(Build.VERSION_CODES.R)
fun Activity.calculateStatusBarHeight(): Int {
    val windowMetrics = windowManager.currentWindowMetrics
    val insets = windowMetrics.windowInsets
        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
    return insets.top
}

@RequiresApi(Build.VERSION_CODES.R)
fun Activity.calculateNavigationBarHeight(): Int {
    val windowMetrics = windowManager.currentWindowMetrics
    val insets = windowMetrics.windowInsets
        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
    return insets.bottom
}

@RequiresApi(Build.VERSION_CODES.R)
fun Activity.getScreenHeight(): Int {
    val windowMetrics = windowManager.currentWindowMetrics
    val insets = windowMetrics.windowInsets
        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
    val heights = insets.top + insets.bottom
    return windowMetrics.bounds.height() + heights
}

@RequiresApi(Build.VERSION_CODES.R)
fun Activity.getScreenWidth(): Int {
    val windowMetrics = windowManager.currentWindowMetrics
    val insets = windowMetrics.windowInsets
        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
    val widths = insets.left + insets.right
    return windowMetrics.bounds.width() + widths
}

fun getLargeRandomNum(): Int {
    return (0..Int.MAX_VALUE).random()
}

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

fun AlbumDetails.toPlaylist(): Playlist {
    return Playlist(name, "", picUrl, description)
}