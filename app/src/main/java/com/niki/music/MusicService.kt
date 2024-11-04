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
import com.niki.common.utils.shuffle
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

interface MusicServiceListener {
    fun onSongChanged(song: Song)
    fun onPlayingStateChanged(isPlaying: Boolean)
    fun onProgressUpdated(newProgress: Double, isReset: Boolean = false)
    fun onPlayModeChanged(newState: Int)
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
    }

    private var mediaPlayer = Player()

    private var _playMode = LOOP
        set(value) {
            if (value !in LOOP..RANDOM) return
            listener?.onPlayModeChanged(value)
            field = value
        }

    private val _isPlaying: Boolean
        get() = getIsPlaying()

    private lateinit var remoteViews: RemoteViews

    private val binder = MusicServiceBinder()
    private var listener: MusicServiceListener? = null

    private val playerModel by lazy { PlayerModel() }

    private val musicScope by lazy { CoroutineScope(Dispatchers.IO) }

    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var playNowJob: Job? = null

    private var playNowJobIsAlive = false

    private var currentPlaylist: MutableList<Song> = mutableListOf()
    private var backupPlaylist: MutableList<Song> = mutableListOf() // 随机序列之前备份

    private var songMap: HashMap<Song, String?> = hashMapOf()

    private var songIndex = 0

    init {
        progressJob = musicScope.launch {
            while (true) {
                if (!_isPlaying) continue

                val totalLen = mediaPlayer.duration // 音频总长度

                if (totalLen <= 0) {
                    notifyResetSeekbar()
                    continue
                }

                val currentLen = mediaPlayer.currentPosition // 播放到的位置
                val progress = (currentLen * 100.0) / totalLen // 得到播放进度的百分比

                listener?.onProgressUpdated(progress)
                delay(250)
            }
        }
    }

    private fun getIsPlaying(): Boolean = try {
        val i = mediaPlayer.isPlaying
        i
    } catch (_: Exception) {
        false
    }

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

    private fun setListener(l: MusicServiceListener?) {
        listener = l
    }

    private fun notifyPlayingStateChanged(isPlaying: Boolean) {
        listener?.onPlayingStateChanged(isPlaying)
        updatePlayingStatus(isPlaying)
    }

    private fun notifySongChanged(song: Song) {
        listener?.onSongChanged(song)
        updateRemoteViews(song)
    }

    private fun notifyResetSeekbar() {
        listener?.onProgressUpdated(0.0, true)
    }

    /**
     * 只要出现错误就修复
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

    fun updatePlaylist(list: List<Song>) {
        songIndex = 0
        currentPlaylist = list.toMutableList()
        backupPlaylist = list.toMutableList()
        handlePlaylists()
        playNow()
    }

    private fun playNow(isRetry: Boolean = false): Any = runCatching {
        reset()
        withCurrentSong { mediaPlayer.playNow(it) }
    }.onSuccess {
        withCurrentSong {
            notifyPlayingStateChanged(true)
            notifySongChanged(it)
        }
    }.onFailure {
        logE("$$$", it.stackTrace.toString())
        if (!isRetry)
            playNow(true)
    }

    private fun play() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception() // 此处抛出异常是不希望进入 on success 块
        mediaPlayer.start()
    }.onSuccess {
        withCurrentSong {
            notifySongChanged(it)
            notifyPlayingStateChanged(true)
        }
    }.onFailure {
        if (it is NotPreparedException)
            "请稍候".toast()
        else {
            logE("$$$", it.stackTrace.toString())
            if (_isPlaying)
                reset()
            else
                playNow()
        }
    }

    private fun pause() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception()
        mediaPlayer.pause()
    }.onSuccess {
        withCurrentSong {
            notifySongChanged(it)
            notifyPlayingStateChanged(false)
        }
    }.onFailure {
        if (it is NotPreparedException)
            "请稍候".toast()
        else {
            logE("$$$", it.stackTrace.toString())
            if (!_isPlaying)
                reset()
        }
    }

    private fun seekToPosition(position: Int) = runCatching {
        val seekToPosition = (position * mediaPlayer.duration) / 100
        if (mediaPlayer.state != PREPARED) return@runCatching
        mediaPlayer.seekTo(seekToPosition)
    }

    private fun changePlayMode() {
        _playMode += 1
        if (_playMode > RANDOM)
            _playMode = LOOP

        handlePlaylists()
    }

    private fun handlePlaylists() {
        if (currentPlaylist.isNotEmpty()) {
            val thisSong =
                currentPlaylist[songIndex] // 为了让索引重新指向当前播放的音乐
            when (_playMode) {
                RANDOM -> shuffle(currentPlaylist)
                else -> currentPlaylist =
                    backupPlaylist.toMutableList()
            }
            songIndex = currentPlaylist.indexOf(thisSong)
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
     * 主动播放并加载下一曲
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
                    logE("$$$", it.stackTrace.toString())
                }
            },
            { _, _ ->
            })
    }

    inner class Player : MediaPlayer() {
        var state = RELEASED
            private set

        init {
            init()
        }

        fun init() = runCatching {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnPreparedListener {
                state = PREPARED
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
                state = ERROR
                true
            }

            state = INIT
        }

        fun playNow(song: Song) {
            playJob?.cancel()
            playNowJob?.cancel()
            playNowJob = musicScope.launch(Dispatchers.IO) {
                playNowJobIsAlive = true
                loadUrl(song) { url ->
                    runCatching {
                        if (!isActive) {
                            playNowJobIsAlive = false
                            return@loadUrl
                        }
                        reset()
                        setDataSource(url)
                        prepare()
                        playNowJobIsAlive = false
                        start()
                    }.onFailure {
                        state = ERROR
                    }
                }
            }
        }

        override fun start() {
            if (playNowJobIsAlive) return
            playJob?.cancel()
            playJob = musicScope.launch {
                var timer = 0
                while (timer <= 500) {
                    if (state != PREPARED) {
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

        override fun pause() {
            if (playNowJobIsAlive) return
            playJob?.cancel()
            playJob = musicScope.launch {
                var timer = 0
                while (timer <= 500) {
                    if (state != PREPARED) {
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

        fun clean() = runCatching {
            seekTo(0)
            stop()
            reset()
            release()
            state = RELEASED
        }.onFailure {
            state = ERROR
        }

        override fun seekTo(msec: Int) {
            if (state != PREPARED) throw NotPreparedException()
            super.seekTo(msec)
        }
    }

    inner class MusicServiceBinder : Binder() {
        fun updatePlaylist(list: List<Song>) = this@MusicService.updatePlaylist(list)
        fun setListener(l: MusicServiceListener?) = this@MusicService.setListener(l)

        fun getLength(): Int = try {
            val d = mediaPlayer.duration
            d
        } catch (_: Exception) {
            -1
        }

        fun getIsPlaying(): Boolean = this@MusicService.getIsPlaying()

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