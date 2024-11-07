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
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.isUrl
import com.niki.common.utils.shuffle
import com.niki.music.mine.appCookie
import com.niki.music.model.PlayerModel
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.extension.toastSuspended
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

interface MusicServiceListener {
    fun onSongChanged(song: Song)
    fun onPlayingStateChanged(isPlaying: Boolean)

    /**
     * 回调进度的百分比
     */
    fun onProgressUpdated(newProgress: Double, isReset: Boolean = false)
    fun onPlayModeChanged(newState: Int)
}

class MusicService : Service() {
    private val TAG = this::class.simpleName!!

    companion object {
        const val CHANNEL_ID = "p1ay1s.music"
        const val NOTIFICATION_ID = 666

        const val ACTION_SWITCH = "switch"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"

        const val SWITCH_CODE = 0
        const val NEXT_CODE = 1
        const val PREVIOUS_CODE = 2

        // play mode
        const val LOOP = 0
        const val SINGLE = 1
        const val RANDOM = 2

        const val NOTIFY_CD = 200L
        const val MAX_RETRY_TIMES = 4
    }

    private var _playMode = LOOP
        set(value) {
            val v = when {
                value in LOOP..RANDOM -> value
                value < LOOP -> RANDOM
                else -> LOOP
            }
            listener?.onPlayModeChanged(v)
            field = v
        }

    private val _isPlaying: Boolean
        get() {
            // 避免由于直接访问导致 mediaplayer 的状态异常
            if (!mediaPlayer.isPrepared) return false
            return mediaPlayer.isPlaying
        }

    private val _songDuration: Int
        get() {
            if (!mediaPlayer.isPrepared) return -1
            return mediaPlayer.duration
        }

    private var listener: MusicServiceListener? = null
    private var mediaPlayer = MusicServicePlayer()

    private lateinit var remoteViews: RemoteViews
    private val binder = MusicServiceBinder()
    private val playerModel by lazy { PlayerModel() }

    private var currentSongs: MutableList<Song> = mutableListOf()
    private var backupSongs: MutableList<Song> = mutableListOf() // 随机序列之前备份

    private val playlistLastIndex: Int
        get() = currentSongs.size - 1

    private var currentIndex = 0

    // 存储已获取到的 url
    private var songUrlMap: HashMap<Song, String?> = hashMapOf()

    /**
     * 为了使用协程的取消功能
     */
    private val singleThreadContext = newSingleThreadContext("music-thread")
    private val musicScope: CoroutineScope by lazy { CoroutineScope(singleThreadContext) }
    private var job: Job? = null

    init {
        // 开启全局的进度条更新协程
        musicScope.launch(Dispatchers.IO) {
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
     * 阻塞线程获取音乐 url
     *
     * 应在 callback 处理空事件以及播放器的重置
     */
    private suspend fun loadUrl(song: Song, callback: ((String?) -> Unit)? = null): String? {
        songUrlMap[song]?.let {
            if (it.isUrl()) {
                callback?.invoke(it)
                return it
            }
        }

        return withContext(singleThreadContext) { // 只有一条线程所以会阻塞直到回调
            var result: String? = null
            playerModel.getSongInfoSuspend(song.id, "jymaster", appCookie,
                {
                    try {
                        result = it.data[0].url
                        songUrlMap[song] = result

                        if (isActive) callback?.invoke(result)
                    } catch (t: Exception) {
                        logE(TAG, t.message + "\n" + t.stackTrace.toString())
                        if (isActive) callback?.invoke(null)
                    }
                },
                { _, _ ->
                    "网络错误, 请重试".toast()
                    if (isActive) callback?.invoke(null)
                })
            result
        }
    }

    private fun notifyPlayingStateChanged(isPlaying: Boolean) =
        musicScope.launch(Dispatchers.Main) {
            updatePlayingStatus(isPlaying)
            listener?.onPlayingStateChanged(isPlaying)
        }

    private fun notifyNewSong() = musicScope.launch(Dispatchers.Main) {
        getCurrentSong()?.let {
            listener?.onSongChanged(it)
            updateRemoteViews(it)
        }
        listener?.onProgressUpdated(0.0, true)
        notifyPlayingStateChanged(_isPlaying)
    }

    /**
     * 重置 media player 以及 view 的状态
     */
    private fun reset() {
        mediaPlayer.free()
        mediaPlayer = MusicServicePlayer()
    }

    private fun fix() {
        reset()
        playNow()
    }

    private fun getCurrentSong(): Song? = try {
        currentSongs[currentIndex]
    } catch (_: Exception) {
        null
    }

    /**
     * 立即重置 mediaplayer 并播放当前指向的一首歌
     */
    private fun playNow(times: Int = MAX_RETRY_TIMES) {
        val song = getCurrentSong() ?: return
        job?.cancel() // 播放一首新的歌曲必然会重置 mediaplayer, 所以可以直接取消前一个 job, 是安全的
        job = musicScope.launch(singleThreadContext) {
            notifyNewSong()
            runCatching {
                reset()
                if (times <= 0) {
                    toastSuspended("播放失败")
                    return@runCatching
                }
                song ?: throw Exception("cannot play with a null song!")
                if (!isActive) return@launch
                mediaPlayer.playNow(song)
            }.onSuccess {
                notifyNewSong()
            }.onFailure {
                if (!isActive) return@launch
                playNow(times - 1)
            }
        }
    }

    private fun play() = musicScope.launch(singleThreadContext) {
        job?.join() // 安全操作 mediaplayer
        job = musicScope.launch(singleThreadContext) {
            runCatching {
                mediaPlayer.start()
            }.onSuccess {
                notifyPlayingStateChanged(true)
            }.onFailure {
                playNow()
            }
        }
    }

    private fun pause() = musicScope.launch(singleThreadContext) {
        job?.join()
        job = musicScope.launch(singleThreadContext) {
            runCatching {
                mediaPlayer.pause()
            }.onSuccess {
                notifyPlayingStateChanged(false)
            }.onFailure {
                playNow()
            }
        }
    }

    private fun plusIndex() {
        currentIndex++
        if (currentIndex > playlistLastIndex) currentIndex = 0
    }

    private fun minusIndex() {
        currentIndex--
        if (currentIndex < 0) currentIndex = playlistLastIndex
    }

    private fun previous() = musicScope.launch(singleThreadContext) {
        if (currentSongs.isEmpty()) return@launch
        minusIndex()
        playNow()
    }

    private fun next() = musicScope.launch(singleThreadContext) {
        if (currentSongs.isEmpty()) return@launch
        plusIndex()
        playNow()
    }

    /**
     * 处理 seekbar 的拖动事件: 如果 mediaplayer 已经就绪则 seekTo 并播放
     */
    private fun seekToPosition(position: Int) = musicScope.launch(singleThreadContext) {
        val seekToPosition = (position * _songDuration) / MainActivity.SEEKBAR_MAX.toInt()
        if (_songDuration > 0) {
            mediaPlayer.seekTo(seekToPosition)
            play()
        } else {
            musicScope.launch(Dispatchers.Main) {
                listener?.onProgressUpdated(0.0, true)
            }
        }
    }

    /**
     * 更换播放模式并相应调整播放队列排序
     */
    private fun changePlayMode() = musicScope.launch(singleThreadContext) {
        _playMode++
        setPlaylists()
    }

    /**
     * 设置新的播放队列
     */
    private fun setNewPlaylist(list: List<Song>) = musicScope.launch(singleThreadContext) {
        list.let {
            currentIndex = 0
            currentSongs = it.toMutableList()
            backupSongs = it.toMutableList()
            setPlaylists()
            playNow()
        }
    }

    /**
     * 根据播放模式设置 list
     */
    private fun setPlaylists() = runCatching {
        if (currentSongs.isEmpty()) return@runCatching

        val thisSong = currentSongs[currentIndex] // 为了让索引重新指向当前播放的音乐
        when (_playMode) {
            RANDOM -> shuffle(currentSongs)

            else -> currentSongs = backupSongs.toMutableList()
        }
        currentIndex = currentSongs.indexOf(thisSong)
    }

    /**
     * 针对音乐服务设置了更安全的操作
     *
     * 通过前置判断抛出异常以避免不必要的 media player 重建
     */
    inner class MusicServicePlayer : MediaPlayer() {
        private var _isPrepared = false
        val isPrepared: Boolean
            get() = _isPrepared

        init {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnCompletionListener {
                if (_playMode == SINGLE) {
                    play()
                } else {
                    next()
                }
            }

            setOnErrorListener { _, _, _ ->
                fix()
                true
            }
        }

        /**
         * 立即获取 url 并播放
         *
         * 需要外界保证已经重置状态
         */
        suspend fun playNow(song: Song) {
            if (isPrepared) throw Exception("have to release before a new play affair!")
            loadUrl(song) { url ->
                setDataSource(url)
                prepare()
                if (!url.isUrl() || getCurrentSong() != song) throw Exception("the song has changed!")
                start()
            }
        }

        override fun prepare() {
            _isPrepared = false
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
     * 用于回调给主活动
     */
    inner class MusicServiceBinder : Binder() {
        val songDuration: Int
            get() = _songDuration
        val isPlaying: Boolean
            get() = _isPlaying

        fun setNewPlaylist(list: List<Song>) = this@MusicService.setNewPlaylist(list)
        fun setMusicServiceListener(l: MusicServiceListener?) {
            listener = l
        }

        fun previous() = this@MusicService.previous()
        fun next() = this@MusicService.next()
        fun seekToPosition(position: Int) = this@MusicService.seekToPosition(position)
        fun switch() {
            if (_isPlaying)
                this@MusicService.pause()
            else
                this@MusicService.play()
        }

        fun changePlayMode() = this@MusicService.changePlayMode()
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
            PendingIntent.getService(this, SWITCH_CODE, iSwitch, PendingIntent.FLAG_MUTABLE)
        val pPrevious =
            PendingIntent.getService(this, PREVIOUS_CODE, iPrevious, PendingIntent.FLAG_MUTABLE)
        val pNext =
            PendingIntent.getService(this, NEXT_CODE, iNext, PendingIntent.FLAG_MUTABLE)

        remoteViews.setOnClickPendingIntent(R.id.ivSwitch, pSwitch)
        remoteViews.setOnClickPendingIntent(R.id.ivPrevious, pPrevious)
        remoteViews.setOnClickPendingIntent(R.id.ivNext, pNext)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐控制栏",
            NotificationManager.IMPORTANCE_DEFAULT
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

    /**
     * 为 remoteviews 设置播放歌曲
     */
    private fun updateRemoteViews(song: Song) {
        remoteViews =
            RemoteViews(packageName, R.layout.remote_control) // 使用同一切换过多后就不会响应(内存问题), 所以尝试每次都重建
        setSongViews(song) {
            refreshRemoteViews()
        }
    }

    /**
     * 为 remoteviews 设置播放状态
     */
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
                    if (getCurrentSong()?.al?.picUrl == imageUrl)
                        remoteViews.setImageViewBitmap(R.id.ivCover, resource)
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