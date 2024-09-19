package com.niki.utils.base

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 自用日志记录
const val VERBOSE = 1
const val DEBUG = 2
const val INFO = 3
const val WARN = 4
const val ERROR = 5

@PublishedApi
internal const val LOG_LEVEL = DEBUG

const val SECOND = 1000L

// 是否启用自动删除
const val CLEAN_OLD = false
const val DAYS_RETAINED = 0
const val HOURS_RETAINED = 0
const val MINUTES_RETAINED = 0
const val SECONDS_RETAINED = 0

// 写入文件的间隔
const val AUTO_WRITE_TIME_INTERVAL = SECOND * 5

// 命名
const val FILE_HEADER = "日志备份-"
const val FILE_PATH = "logs"
const val FILE_TYPE = ".txt"
const val DATE_FORMAT = "yyyy年MM月dd日"

// 日志偏好
const val LOG_HEADER = ""
const val TIME_FORMAT = "MM/dd HH:mm:ss"

inline fun getFunctionName(): String {
    val functionName = object {}.javaClass.enclosingMethod?.name
    if (functionName != "getFunctionName") {
        return "$functionName: "
    } else {
        return ""
    }
}

private val levels = mapOf(
    VERBOSE to "VERBOSE",
    DEBUG to "DEBUG",
    INFO to "INFO",
    WARN to "WARN",
    ERROR to "ERROR",
)

fun getName(level: Int): String = levels[level]!!

object Logger {
    private val TAG = this::class.simpleName!!
    private lateinit var fileDir: File
    private lateinit var file: File

    private var accurateTimeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
    private var dateFormat: String =
        SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
    private var fileName = FILE_HEADER + dateFormat + FILE_TYPE

    private val logBuffer = StringBuffer()

    private val scope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logJob: Job? = null

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    private val loggerHandler: ((Thread, Throwable) -> Unit) = { thread, throwable ->
        val msg = "at: ${thread.name}\nmessage: ${throwable.message}\ndetails: ${
            Log.getStackTraceString(throwable)
        }"
        appendLog(getName(ERROR), "UncaughtException", msg)
        writeToFile()

        /**
         * 写入后交给默认的handler接管
         */
        defaultHandler?.uncaughtException(thread, throwable)
    }

    /**
     * 必须在 application 中调用以更快地初始化, 否则不能保证工作
     */
    fun init() {
        create()
        startLogCoroutine()
        registerHandler()
        cleanOldLogs()
    }

    private fun create() {
        fileDir = File(appContext.getExternalFilesDir(null), FILE_PATH)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        fileName = FILE_HEADER + dateFormat + FILE_TYPE
        file = File(fileDir, fileName)
    }

    private fun startLogCoroutine() {
        if (logJob == null || logJob?.isCancelled == true) {
            logJob = scope.launch {
                while (isActive) {
                    delay(AUTO_WRITE_TIME_INTERVAL)
                    writeToFile()
                }
            }
        }
    }

    fun stopLogCoroutine() {
        logJob?.cancel()
        scope.cancel()
    }

    private fun writeToFile() {
        try {
            val file = getLogFile()
            FileWriter(file, true).use { writer ->
                writer.append(logBuffer.toString())
                logBuffer.setLength(0)
            }
        } catch (e: Exception) {
            logE(TAG, "日志写入失败")
        }
    }

    fun appendLog(level: String, tag: String, message: String) {
        val currentTime = accurateTimeFormat.format(Date())
        val logMessage = "$currentTime $level $tag\n$message\n"
        logBuffer.append(logMessage)
    }

    private fun getLogFile(): File {
        val fileName =
            FILE_HEADER + dateFormat + FILE_TYPE
        /**
         * 判断名字主要是为了保证日期一致
         */
        if (fileName != Logger.fileName)
            create()
        return file
    }

    private fun registerHandler() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    loggerHandler(Looper.getMainLooper().thread, e)
                }
            }
        }

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            loggerHandler(t, e)
        }
    }

    private fun cleanOldLogs() {
        if (!CLEAN_OLD) return
        val outOfDate =
            System.currentTimeMillis() - formatTime()
        fileDir.listFiles()?.forEach { file -> // 遍历路径的文件
            if (file.lastModified() < outOfDate) {
                logI(TAG, "删除过期的备份文件: ${file.name}")
                file.delete()
            }
        }
    }

    private fun formatTime(): Long {
        return (DAYS_RETAINED * 24 * 60 * 60 + HOURS_RETAINED * 60 * 60 + MINUTES_RETAINED * 60 + SECONDS_RETAINED) * SECOND
    }
}

inline fun logV(tag: String = "", msg: String = "") {
    with(VERBOSE) {
        if (LOG_LEVEL <= this) {
            Log.v(tag, LOG_HEADER + getFunctionName() + msg)
            Logger.appendLog(getName(this), tag, LOG_HEADER + msg)
        }
    }
}

inline fun logD(tag: String = "", msg: String = "") {
    with(DEBUG) {
        if (LOG_LEVEL <= this) {
            Log.d(tag, LOG_HEADER + getFunctionName() + msg)
            Logger.appendLog(getName(this), tag, LOG_HEADER + msg)
        }
    }
}

inline fun logI(tag: String = "", msg: String = "") {
    with(INFO) {
        if (LOG_LEVEL <= this) {
            Log.i(tag, LOG_HEADER + getFunctionName() + msg)
            Logger.appendLog(getName(this), tag, LOG_HEADER + msg)
        }
    }
}

inline fun logW(tag: String = "", msg: String = "") {
    with(WARN) {
        if (LOG_LEVEL <= this) {
            Log.w(tag, LOG_HEADER + getFunctionName() + msg)
            Logger.appendLog(getName(this), tag, LOG_HEADER + msg)
        }
    }
}

inline fun logE(tag: String = "", msg: String = "") {
    with(ERROR) {
        if (LOG_LEVEL <= this) {
            Log.e(tag, LOG_HEADER + getFunctionName() + msg)
            Logger.appendLog(getName(this), tag, LOG_HEADER + msg)
        }
    }
}

inline fun logDoing(stepName: String) {
    with(ERROR) {
        if (LOG_LEVEL <= this) {
            Log.e(stepName, "Doing $stepName")
            Logger.appendLog(getName(this), stepName, "Doing $stepName")
        }
    }
}

