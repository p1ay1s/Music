package com.niki.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.isUrl
import com.niki.common.utils.shuffle
import com.niki.music.MusicService.Companion.ACTION_NEXT
import com.niki.music.MusicService.Companion.ACTION_PREVIOUS
import com.niki.music.MusicService.Companion.ACTION_SWITCH
import com.niki.music.MusicService.Companion.CHANNEL_ID
import com.niki.music.MusicService.Companion.LOOP
import com.niki.music.MusicService.Companion.NOTIFICATION_ID
import com.niki.music.MusicService.Companion.NOTIFY_CD
import com.niki.music.MusicService.Companion.RANDOM
import com.niki.music.MusicService.Companion.SINGLE
import com.niki.music.mine.appCookie
import com.niki.music.model.PlayerModel
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock

interface MusicServiceListener {
    fun onSongChanged(song: Song)
    fun onPlayingStateChanged(isPlaying: Boolean)

    /**
     * 回调进度的百分比
     */
    fun onProgressUpdated(newProgress: Double, isReset: Boolean = false)
    fun onPlayModeChanged(newState: Int)
}

class SafeMusicService : Service() {
    private val TAG = this::class.simpleName!!
    private val handlerThread = HandlerThread("music service thread").also {
        it.start()
    }
    private val handler = object : Handler(handlerThread.looper) { // 在初始化前必须先启动线程
        override fun handleMessage(msg: Message) = handle(msg)
    }

    companion object {
        const val SWITCH = 1
        const val PREVIOUS = 2
        const val NEXT = 3

        const val SEEK = 4
        const val CHANGE_MODE = 5
        const val UPDATE_LIST = 6

        const val MAX_RETRY_TIMES = 2
    }

    private var _playMode = LOOP
        set(value) {
            val v = when {
                value in LOOP..RANDOM -> value
                value < LOOP -> RANDOM
                else -> LOOP
            }
            mainHandler.post {
                listener?.onPlayModeChanged(v)
            }
            field = v
        }

    private val _isPlaying: Boolean
        get() {
            if (!mediaPlayer.isPrepared) return false
            return mediaPlayer.isPlaying
        }

    private val _songDuration: Int
        get() {
            if (!mediaPlayer.isPrepared) return -1
            return mediaPlayer.duration
        }

    private var listener: MusicServiceListener? = null
    private var mediaPlayer = SafeMediaPlayer()

    private lateinit var remoteViews: RemoteViews
    private val binder = SafeMusicServiceBinder()
    private val playerModel by lazy { PlayerModel() }

    private var currentPlaylist: MutableList<Song> = mutableListOf()
    private var backupPlaylist: MutableList<Song> = mutableListOf() // 随机序列之前备份

    private val playlistLastIndex: Int
        get() = currentPlaylist.size - 1

    private var songIndex = 0

    // 存储已获取到的 url
    private var songMap: HashMap<Song, String?> = hashMapOf()

    private val mainHandler: Handler
        get() = Handler(Looper.getMainLooper())

    private val scope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    init {
        // 开启全局的进度条更新协程
        scope.launch(Dispatchers.Main) {
            while (true) {
                delay(NOTIFY_CD)

                if (!_isPlaying)
                    continue

                val totalLen = _songDuration // 音频总长度
                if (totalLen <= 0) {
                    listener?.onProgressUpdated(0.0, true)
                    continue
                }

                val currentLen = mediaPlayer.currentPosition // 播放到的位置
                val progress = currentLen.toDouble() / totalLen // 得到播放进度的百分比

                listener?.onProgressUpdated(progress)
            }
        }
    }

    /**
     * 加载音乐 url 并回调
     *
     * 应在 callback 处理空事件以及播放器的重置
     */
    val lock = ReentrantLock()
    private fun loadUrl(song: Song, callback: ((String?, CountDownLatch?) -> Unit)? = null) {
        // 当未设置 callback 则不阻塞
        val latch = if (callback != null) CountDownLatch(1) else null
        var url: String? = songMap[song]
        if (!url.isNullOrBlank()) {
            if (getCurrentSong() == song) callback?.invoke(url, latch)
            return
        }

        playerModel.getSongInfo(song.id, "jymaster", appCookie,
            {
                try {
                    url = it.data[0].url
                    songMap[song] = url
                    if (getCurrentSong() == song && !url.isNullOrBlank())
                        callback?.invoke(url, latch)
                } catch (t: Exception) {
                    logE(TAG, t.message + "\n" + t.stackTrace.toString())
                }
            },
            { _, _ ->
                if (callback != null) "网络错误, 请重试".toast()
                callback?.invoke(null, latch)
            })

        latch?.await()
    }

    private fun notifyPlayingStateChanged(isPlaying: Boolean) =
        mainHandler.post {
//        scope.launch(Dispatchers.Main) {
//            updatePlayingStatus(isPlaying)
            listener?.onPlayingStateChanged(isPlaying)
        }
//        }

    private fun notifyReset() =
        mainHandler.post {
//        scope.launch(Dispatchers.Main) {
            listener?.onProgressUpdated(0.0, true)
            notifyPlayingStateChanged(false)
        }
//        }

    private fun notifyPlayNewSong(song: Song?) =
        mainHandler.post {
//        scope.launch(Dispatchers.Main) {
            song?.let {
                listener?.onSongChanged(song)
//                updateRemoteViews(song)
            }
            listener?.onProgressUpdated(0.0, true)
            notifyPlayingStateChanged(true)
        }
//        }

    /**
     * 重置 media player 以及 view 的状态
     */
    private fun reset() {
        notifyReset()
        mediaPlayer.free()
        mediaPlayer = SafeMediaPlayer()
    }

    private fun getCurrentSong(): Song? {
        try {
            val song = currentPlaylist[songIndex]
            return song
        } catch (_: Exception) {
            return null
        }
    }

    private fun getCurrentUrl(): String? {
        val s = getCurrentSong() ?: return null
        return songMap[s]
    }

    private fun withCurrentSong(block: (Song) -> Unit) = getCurrentSong()?.let(block)

    private fun withCurrentUrl(block: (String) -> Unit) = getCurrentUrl()?.let(block)

    private fun playNow(
        times: Int = MAX_RETRY_TIMES,
        song: Song? = getCurrentSong(),
        onFailure: (() -> Unit)? = null
    ): Any =
        runCatching {
            if (times <= 0) return@runCatching
            reset()
            song ?: throw Exception("cannot play with a null song!")
            mediaPlayer.playNow(song)
        }.onSuccess {
            notifyPlayNewSong(song)
        }.onFailure {
            onFailure?.invoke()
            playNow(times - 1)
        }

    private fun play() = runCatching {
        mediaPlayer.start()
    }.onSuccess {
        notifyPlayingStateChanged(true)
    }.onFailure {
        playNow()
    }

    private fun pause() = runCatching {
        mediaPlayer.pause()
    }.onSuccess {
        notifyPlayingStateChanged(false)
    }.onFailure {
        playNow()
    }

    private fun plusIndex() {
        songIndex++
        if (songIndex > playlistLastIndex) songIndex = 0
    }

    private fun minusIndex() {
        songIndex--
        if (songIndex < 0) songIndex = playlistLastIndex
    }

    private fun previous() {
        if (currentPlaylist.isEmpty()) return
        minusIndex()
        playNow(onFailure = { plusIndex() })
    }

    private fun next() {
        if (currentPlaylist.isEmpty()) return
        plusIndex()
        playNow(onFailure = { minusIndex() })
    }

    private fun seek(position: Any) {
        if (position !is Int) return

        val seekToPosition = (position * _songDuration) / MainActivity.SEEKBAR_MAX.toInt()
        if (_songDuration > 0)
            mediaPlayer.seekTo(seekToPosition)
        else
//            mainHandler.post {
            scope.launch(Dispatchers.Main) {
                listener?.onProgressUpdated(0.0, true)
//                }
            }
    }

    private fun changeMode() {
        _playMode++
        handlePlaylists()
    }

    private fun updateList(list: Any) {
        val songs = list as? List<*>
        if (songs.isNullOrEmpty() || songs.first() !is Song) return

        @Suppress("UNCHECKED_CAST")
        (songs as List<Song>).let {
            songIndex = 0
            currentPlaylist = it.toMutableList()
            backupPlaylist = it.toMutableList()
            handlePlaylists()
            playNow()
        }
    }

    /**
     * 根据播放模式设置 list
     */
    private fun handlePlaylists() {
        if (currentPlaylist.isEmpty()) return

        val thisSong =
            currentPlaylist[songIndex] // 为了让索引重新指向当前播放的音乐
        when (_playMode) {
            RANDOM -> shuffle(currentPlaylist)

            else -> {
                currentPlaylist =
                    backupPlaylist.toMutableList()
                songIndex = currentPlaylist.indexOf(thisSong)
            }
        }
    }

    inner class SafeMediaPlayer : MediaPlayer() {
        private var _isPrepared = false
        val isPrepared: Boolean
            get() = _isPrepared

        init {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )

            setOnCompletionListener {
                if (_playMode == SINGLE) {
                    play()
                } else {
                    next()
                }
            }

            setOnErrorListener { _, _, _ ->
                free()
                true
            }
        }

        /**
         * 立即获取 url 并播放
         *
         * 需要外界保证已经重置状态
         */
        fun playNow(song: Song) {
            logE("$$$", "playNow - start")
            if (isPrepared) throw Exception("have to release before a new play affair!")
            logE("$$$", "loadUrl " + Looper.myLooper().toString())
            loadUrl(song) { url, latch ->
                logE("$$$", "callback " + Looper.myLooper().toString())
                if (!url.isUrl() || getCurrentSong() != song) {
                    logE("$$$", "playNow - end")
                    latch?.countDown()
                    return@loadUrl
                }
                setDataSource(url)
                prepare()
                start()
                logE("$$$", "playNow - end")
                latch?.countDown()
            }
        }

        override fun prepare() {
            super.prepare()
            _isPrepared = true
        }

        override fun start() {
            if (!isPrepared) throw Exception("not prepared!")
            super.start()
        }

        override fun pause() {
            if (!isPrepared) throw Exception("not prepared!")
            super.pause()
        }

        /**
         * 此方法会导致当前实例失去所有监听器
         */
        fun free() = runCatching {
            _isPrepared = false
            stop()
            reset()
            release()
        }

        override fun seekTo(msec: Int) {
            if (!isPrepared) throw Exception("not prepared!")
            super.seekTo(msec)
        }
    }

    /**
     * @不要动下面的
     ********************************************************************************************************************************************
     ********************************************************************************************************************************************
     ********************************************************************************************************************************************
     ********************************************************************************************************************************************
     ********************************************************************************************************************************************
     ********************************************************************************************************************************************
     */

    inner class SafeMusicServiceBinder : Binder() {
        val songDuration: Int
            get() = _songDuration
        val isPlaying: Boolean
            get() = _isPlaying

        fun updatePlaylist(list: List<Song>) = send(UPDATE_LIST, list)
        fun setListener(l: MusicServiceListener?) {
            listener = l
        }

        fun previous() = send(PREVIOUS)
        fun next() = send(NEXT)
        fun seekToPosition(position: Int) = send(SEEK, position)
        fun changePlayMode() = send(CHANGE_MODE)
        fun switch() = send(SWITCH)
    }

    private fun send(what: Int, obj: Any? = null) {
        val message = handler.obtainMessage()
        message.obj = obj
        message.what = what
        handler.sendMessage(message)
    }

    private fun handle(msg: Message) {
        logE("$$$", Looper.myLooper().toString())
        when (msg.what) {
            SWITCH -> {
                if (_isPlaying)
                    pause()
                else
                    play()
            }

            PREVIOUS -> previous()
            NEXT -> next()

            SEEK -> seek(msg.obj)
            CHANGE_MODE -> changeMode()
            UPDATE_LIST -> updateList(msg.obj)

            else -> logE(TAG, "unexpected msg with what[${msg.what}]")
        }
    }

    override fun onCreate() {
        super.onCreate()
        remoteViews = RemoteViews(packageName, R.layout.remote_control)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )
        }
    }

    private fun createNotification(): Notification {
        val iSwitch = Intent(this, SafeMusicService::class.java)
        iSwitch.action = ACTION_SWITCH
        val iPrevious = Intent(this, SafeMusicService::class.java)
        iPrevious.action = ACTION_PREVIOUS
        val iNext = Intent(this, SafeMusicService::class.java)
        iNext.action = ACTION_NEXT

        val pSwitch =
            PendingIntent.getService(this, 0, iSwitch, PendingIntent.FLAG_MUTABLE)
        val pPrevious =
            PendingIntent.getService(this, 1, iPrevious, PendingIntent.FLAG_MUTABLE)
        val pNext =
            PendingIntent.getService(this, 2, iNext, PendingIntent.FLAG_MUTABLE)

        remoteViews.setOnClickPendingIntent(R.id.ivSwitch, pSwitch)
        remoteViews.setOnClickPendingIntent(R.id.ivPrevious, pPrevious)
        remoteViews.setOnClickPendingIntent(R.id.ivNext, pNext)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐控制栏",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        ).setSmallIcon(R.drawable.ni)
            .setCustomContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return notification
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun updateRemoteViews(song: Song) {
        remoteViews = RemoteViews(packageName, R.layout.remote_control) // 切换过多后就不会响应, 尝试每次都重建
        setSongViews(song) {
            refreshRemoteViews()
        }
    }

    private fun updatePlayingStatus(isPlaying: Boolean?) {
        if (!::remoteViews.isInitialized) return
        if (isPlaying == true)
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_pause)
        else
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_play)
        refreshRemoteViews()
    }

    /**
     * 不要调用 , 刷新 remote views 请使用 updateRemoteViews
     */
    private fun setSongViews(song: Song, callback: () -> Unit) {
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
        if (getCurrentSong() != song) return
        remoteViews.setTextViewText(R.id.tvSongName, song.name)
        remoteViews.setTextViewText(R.id.tvSinger, builder.toString())
        updatePlayingStatus(_isPlaying)
        setCoverView(song.al.picUrl, callback)
    }

    /**
     * 不要调用 , 刷新 remote views 请使用 updateRemoteViews
     */
    private fun setCoverView(
        imageUrl: String,
        callback: () -> Unit
    ) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .override(200)
            .transform(RoundedCorners(35))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    withCurrentSong { // 避免前面的歌曲后回调
                        if (it.al.picUrl == imageUrl) {
                            remoteViews.setImageViewBitmap(R.id.ivCover, resource)
                        }
                    }
                    callback()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    callback()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    callback()
                }
            })
    }

    /**
     * 不要调用
     */
    private fun refreshRemoteViews() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
}

/**
 * 改名一定要看看清单里有没有改成功
 */

class MusicService : Service() {
    companion object {
        const val CHANNEL_ID = "p1ay1s.music"
        const val NOTIFICATION_ID = 666

        const val ACTION_SWITCH = "switch"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"

        // player state
        private const val RELEASED = 0
        private const val INIT = 1
        private const val PREPARED = 2
        private const val ERROR = 3

        // play mode
        const val LOOP = 0
        const val SINGLE = 1
        const val RANDOM = 2

        const val NOTIFY_CD = 200L
        const val MAX_RETRY_TIME = 3
    }

    private val TAG = this::class.simpleName!!

    private var mediaPlayer = Player()

    private var _playMode = LOOP
        set(value) {
            val v = when {
                value < LOOP -> RANDOM
                value in LOOP..RANDOM -> value
                else -> LOOP
            }
            listener?.onPlayModeChanged(v)
            field = v
        }

    private val _isPlaying: Boolean
        get() = try {
            val i = mediaPlayer.isPlaying
            i
        } catch (_: Exception) {
            false
        }

    private val _length: Int
        get() = try {
            if (mediaPlayer.state != PREPARED) throw NotPreparedException()
            val d = mediaPlayer.duration
            d
        } catch (_: Exception) {
            -1
        }

    private lateinit var remoteViews: RemoteViews

    private val binder = MusicServiceBinder()
    private var listener: MusicServiceListener? = null

    private val playerModel by lazy { PlayerModel() }

    private val musicScope by lazy { CoroutineScope(Dispatchers.IO) }

    private var playJob: Job? = null
    private var playNowJob: Job? = null

    private var playNowJobIsAlive = false

    private var currentPlaylist: MutableList<Song> = mutableListOf()
    private var backupPlaylist: MutableList<Song> = mutableListOf() // 随机序列之前备份

    private var songIndex = 0

    // 存储已获取到的 url
    private var songMap: HashMap<Song, String?> = hashMapOf()

    init {
        // 开启全局的进度条更新协程
        musicScope.launch {
            while (true) {
                delay(NOTIFY_CD)

                if (!_isPlaying)
                    continue

                val totalLen = _length // 音频总长度
                if (totalLen <= 0) {
                    notifyResetSeekbar()
                    continue
                }

                val currentLen = mediaPlayer.currentPosition // 播放到的位置
                val progress = currentLen.toDouble() / totalLen // 得到播放进度的百分比

                listener?.onProgressUpdated(progress)
            }
        }
    }

    /**
     * 注册点击事件 intent
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_SWITCH ->
                if (_isPlaying)
                    pause()
                else
                    play()

            ACTION_PREVIOUS -> previous()

            ACTION_NEXT -> next()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    inner class MusicServiceBinder : Binder() {
        fun updatePlaylist(list: List<Song>) = this@MusicService.updatePlaylist(list)
        fun setListener(l: MusicServiceListener?) {
            listener = l
        }

        fun getLength(): Int = _length

        fun getIsPlaying(): Boolean = _isPlaying

        fun previous() = this@MusicService.previous()
        fun next() = this@MusicService.next()
        fun seekToPosition(position: Int) = this@MusicService.seekToPosition(position)
        fun changePlayMode() = this@MusicService.changePlayMode()
        fun play() {
            if (_isPlaying)
                this@MusicService.pause()
            else
                this@MusicService.play()
        }
    }

    private fun notifyPlayingStateChanged(isPlaying: Boolean) {
        updatePlayingStatus(isPlaying)
        listener?.onPlayingStateChanged(isPlaying)
    }

    private fun notifySongChanged(song: Song) {
        updateRemoteViews(song)
        listener?.onSongChanged(song)
    }

    private fun notifyResetSeekbar() {
        listener?.onProgressUpdated(0.0, true)
    }

    /**
     * 重置 media player
     */
    private fun reset() {
        mediaPlayer.clean()
        mediaPlayer = Player()
        notifyResetSeekbar()
        notifyPlayingStateChanged(false)
    }

    private fun getCurrentSong(): Song? {
        try {
            val song = currentPlaylist[songIndex]
            return song
        } catch (_: Exception) {
            return null
        }
    }

    private fun getCurrentUrl(): String? {
        val s = getCurrentSong() ?: return null
        return songMap[s]
    }

    private fun withCurrentSong(block: (Song) -> Unit) = getCurrentSong()?.let(block)

    private fun withCurrentUrl(block: (String) -> Unit) = getCurrentUrl()?.let(block)

    /**
     * 传入新的播放列表, 并根据模式设置排序
     */
    fun updatePlaylist(list: List<Song>) {
        songIndex = 0
        currentPlaylist = list.toMutableList()
        backupPlaylist = list.toMutableList()
        handlePlaylists()
        playNow()
    }

    /**
     * 立即进行播放, 失败后进行一次重试
     */
    private fun playNow(retryValue: Int = MAX_RETRY_TIME): Any = runCatching {
        reset()
        withCurrentSong { mediaPlayer.playNow(it) }
    }.onSuccess {
        withCurrentSong {
            notifySongChanged(it)
            notifyPlayingStateChanged(true)
        }
    }.onFailure {
        logE(TAG, it.message + "\n" + it.stackTrace.toString())
        if (retryValue > 0)
            playNow(retryValue - 1)
    }

    /**
     * 直接播放, 通常在 player 已经准备好后调用
     */
    private fun play() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception() // 此处抛出异常是不希望进入 on success 块
        mediaPlayer.start()
    }.onSuccess {
        withCurrentSong {
            notifySongChanged(it)
            notifyPlayingStateChanged(true)
        }
    }.onFailure {
        logE(TAG, it.message + "\n" + it.stackTrace.toString())
        if (_isPlaying)
            reset()
        else
            playNow()
    }

    /**
     * 直接暂停, 通常在 player 已经准备好后调用
     */
    private fun pause() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception()
        mediaPlayer.pause()
    }.onSuccess {
        withCurrentSong {
            notifySongChanged(it)
            notifyPlayingStateChanged(false)
        }
    }.onFailure {
        logE(TAG, it.message + "\n" + it.stackTrace.toString())
        if (!_isPlaying)
            reset()
    }

    /**
     * 设置 media player 的播放位置
     */
    private fun seekToPosition(position: Int) = runCatching {
        val seekToPosition = (position * _length) / MainActivity.SEEKBAR_MAX.toInt()
        if (_length > 0)
            mediaPlayer.seekTo(seekToPosition)
        else
            notifyResetSeekbar()
    }.onFailure {
        logE(TAG, it.message + "\n" + it.stackTrace.toString())
        if (_isPlaying)
            reset()
        else
            playNow()
    }

    /**
     * 更换播放模式
     */
    private fun changePlayMode() {
        _playMode += 1
        if (_playMode > RANDOM)
            _playMode = LOOP

        handlePlaylists()
    }

    /**
     * 根据播放模式设置 list
     */
    private fun handlePlaylists() {
        if (currentPlaylist.isEmpty()) return

        val thisSong =
            currentPlaylist[songIndex] // 为了让索引重新指向当前播放的音乐
        when (_playMode) {
            RANDOM -> musicScope.launch {
                shuffle(currentPlaylist)
                songIndex = currentPlaylist.indexOf(thisSong)
            }

            else -> {
                currentPlaylist =
                    backupPlaylist.toMutableList()
                songIndex = currentPlaylist.indexOf(thisSong)
            }
        }
    }

    /**
     * 播放下一曲
     */
    private fun next() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        songIndex++
        if (songIndex > currentPlaylist.size - 1) songIndex = 0
        playNow()
    }.onSuccess {
        withCurrentSong {
            notifySongChanged(it)
            notifyPlayingStateChanged(true)
        }
    }

    /**
     * 播放上一曲
     */
    private fun previous() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        songIndex--
        if (songIndex < 0) songIndex =
            currentPlaylist.size - 1
        playNow()
    }.onSuccess {
        withCurrentSong {
            notifySongChanged(it)
            notifyPlayingStateChanged(true)
        }
    }

    /**
     * 加载音乐 url 并回调
     */
    private suspend fun loadUrl(song: Song, callback: (String) -> Unit) {
        songMap[song]?.let {
            withCurrentSong { c ->
                if (c == song) callback(it)
            }
        } ?: playerModel.getSongInfoExecute(song.id, "jymaster", appCookie,
            {
                runCatching {
                    val url = it.data[0].url
                    songMap[song] = url
                    withCurrentSong { c ->
                        if (c == song) callback(url)
                    }
                }.onFailure {
                    logE(TAG, it.message + "\n" + it.stackTrace.toString())
                }
            },
            { _, _ ->
                "网络错误, 请重试".toast()
                if (song != getCurrentSong()) return@getSongInfoExecute
                notifyResetSeekbar()
                notifyPlayingStateChanged(false)
                reset()
            })
    }

    inner class Player : MediaPlayer() {
        private var _state = INIT
        val state: Int
            get() = _state

        init {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnPreparedListener {
                _state = PREPARED
            }

            setOnCompletionListener {
                notifyResetSeekbar()
                if (_playMode == SINGLE) {
                    play()
                } else {
                    next()
                }
            }

            setOnErrorListener { _, _, _ ->
                _state = ERROR
                reset()
                true
            }
        }

        /**
         * 立即获取 url 并播放
         *
         * 优先级高于 play & pause(当 playNow 执行中不允许他们干扰)
         */
        fun playNow(song: Song) {
            playJob?.cancel()
            playNowJob?.cancel()
            playNowJob = musicScope.launch(Dispatchers.IO) {
                playNowJobIsAlive = true
                loadUrl(song) { url ->
                    runCatching {
                        if (!isActive || song != getCurrentSong()) {
                            playNowJobIsAlive = false
                            return@loadUrl
                        }
                        reset()
                        setDataSource(url)
                        prepare()
                        playNowJobIsAlive = false
                        start()
                    }.onFailure {
                        _state = ERROR
                    }
                }
            }
        }

        /**
         * prepare 的回调有一定延迟, 此处使用协程检查
         */
        override fun start() {
            if (playNowJobIsAlive) return
            playJob?.cancel()
            playJob = musicScope.launch {
                var timer = 0
                while (timer <= 500) {
                    if (_state != PREPARED) {
                        delay(100)
                        timer += 100
                    } else {
                        if (!isPlaying)
                            super.start()
                        else
                            this@MusicService.playNow()
                        return@launch
                    }
                }
            }
        }

        /**
         * prepare 的回调有一定延迟, 此处使用协程检查
         */
        override fun pause() {
            if (playNowJobIsAlive) return
            playJob?.cancel()
            playJob = musicScope.launch {
                var timer = 0
                while (timer <= 500) {
                    if (_state != PREPARED) {
                        delay(100)
                        timer += 100
                    } else {
                        if (isPlaying)
                            super.pause()
                        else
                            this@MusicService.playNow()
                        return@launch
                    }
                }
            }
        }

        /**
         * 释放
         */
        fun clean() = runCatching {
            stop()
            reset()
            release()
            _state = RELEASED
        }.onFailure {
            _state = ERROR
        }

        override fun seekTo(msec: Int) {
            if (_state != PREPARED) throw NotPreparedException()
            super.seekTo(msec)
        }
    }

    override fun onCreate() {
        super.onCreate()
        remoteViews = RemoteViews(packageName, R.layout.remote_control)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )
        }
    }

    private fun createNotification(): Notification {
        val iSwitch = Intent(this, MusicService::class.java)
        iSwitch.action = ACTION_SWITCH
        val iPrevious = Intent(this, MusicService::class.java)
        iPrevious.action = ACTION_PREVIOUS
        val iNext = Intent(this, MusicService::class.java)
        iNext.action = ACTION_NEXT

        val pSwitch =
            PendingIntent.getService(this, 0, iSwitch, PendingIntent.FLAG_MUTABLE)
        val pPrevious =
            PendingIntent.getService(this, 1, iPrevious, PendingIntent.FLAG_MUTABLE)
        val pNext =
            PendingIntent.getService(this, 2, iNext, PendingIntent.FLAG_MUTABLE)

        remoteViews.setOnClickPendingIntent(R.id.ivSwitch, pSwitch)
        remoteViews.setOnClickPendingIntent(R.id.ivPrevious, pPrevious)
        remoteViews.setOnClickPendingIntent(R.id.ivNext, pNext)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐控制栏",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        ).setSmallIcon(R.drawable.ni)
            .setCustomContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return notification
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun updateRemoteViews(song: Song) {
        remoteViews = RemoteViews(packageName, R.layout.remote_control) // 切换过多后就不会响应, 尝试每次都重建
        setSongViews(song) {
            refreshRemoteViews()
        }
    }

    private fun updatePlayingStatus(isPlaying: Boolean?) {
        if (!::remoteViews.isInitialized) return
        if (isPlaying == true)
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_pause)
        else
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_play)
        refreshRemoteViews()
    }

    /**
     * 不要调用 , 刷新 remote views 请使用 updateRemoteViews
     */
    private fun setSongViews(song: Song, callback: () -> Unit) {
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
        if (getCurrentSong() != song) return
        remoteViews.setTextViewText(R.id.tvSongName, song.name)
        remoteViews.setTextViewText(R.id.tvSinger, builder.toString())
        updatePlayingStatus(_isPlaying)
        setCoverView(song.al.picUrl, callback)
    }

    /**
     * 不要调用 , 刷新 remote views 请使用 updateRemoteViews
     */
    private fun setCoverView(
        imageUrl: String,
        callback: () -> Unit
    ) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .override(200)
            .transform(RoundedCorners(35))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    withCurrentSong { // 避免前面的歌曲后回调
                        if (it.al.picUrl == imageUrl) {
                            remoteViews.setImageViewBitmap(R.id.ivCover, resource)
                        }
                    }
                    callback()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    callback()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    callback()
                }
            })
    }

    /**
     * 不要调用
     */
    private fun refreshRemoteViews() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
}

class NotPreparedException : Exception()