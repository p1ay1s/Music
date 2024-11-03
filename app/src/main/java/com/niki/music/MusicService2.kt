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
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.shuffle
import com.niki.music.mine.appCookie
import com.niki.music.model.PlayerModel
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface MusicServiceListener2 {
    fun onPlayingStateChanged(song: Song, isPlaying: Boolean)
    fun onProgressUpdated(newProgress: Int)
    fun onPlayModeChanged(newState: Int)
}

class MusicService2 : Service() {
    companion object {
        const val CHANNEL_ID = "p1ay1s.music"
        const val NOTIFICATION_ID = 666

        const val ACTION_SWITCH = "switch"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"

        // play mode
        const val LOOP = 0
        const val SINGLE = 1
        const val RANDOM = 2
    }

    private lateinit var remoteViews: RemoteViews

    private val binder = MusicServiceBinder()
    private var listener: MusicServiceListener2? = null

    private var _isPlaying = MutableLiveData(false)
    private var _playMode = MutableLiveData(LOOP)
    private var _song = MutableLiveData<Song>(null)

    private val playerModel by lazy { PlayerModel() }
    private val musicScope by lazy { CoroutineScope(Dispatchers.IO) }
    private var progressJob: Job? = null
    private var loadJob: Job? = null

    private var mediaPlayer = Player()

    private var currentPlaylist: MutableList<Song> = mutableListOf()
    private var backupPlaylist: MutableList<Song> = mutableListOf() // 随机序列之前备份

    private var songMap: HashMap<Song, String?> = hashMapOf()

    private var songIndex = 0


    init {
        _isPlaying.observeForever { isPlaying ->
            updatePlayingStatus(isPlaying)
        }
        _playMode.observeForever {
            listener?.onPlayModeChanged(it)
        }
        _song.observeForever { song ->
            if (song != null) fetchUrl(song)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_SWITCH ->
                if (_isPlaying.value == true)
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
            updatePlayingStatus(_isPlaying.value)
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

    private fun setListener(l: MusicServiceListener2?) {
        listener = l
    }

    private fun getCurrentSong(): Song? {
        try {
            val song = currentPlaylist[songIndex]
            _song.value = song
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

    private inline fun playAction(
        crossinline action: () -> Unit,
        crossinline onSuccess: () -> Unit
    ) = runCatching {
        action()
    }.onFailure {
        it.printStackTrace()
        mediaPlayer.clear()
        logE("$$$", "FAILED")
    }.onSuccess {
        onSuccess()
    }

    fun updatePlaylist(list: List<Song>) {
        songIndex = 0
        currentPlaylist = list.toMutableList()
        backupPlaylist = list.toMutableList()
        formatPlaylists()
        playNew()
    }

    /**
     * 根据播放模式调整歌单排序
     */
    private fun formatPlaylists() {
        if (currentPlaylist.isNotEmpty()) {
            val thisSong = getCurrentSong()
            when (_playMode.value) {
                RANDOM -> shuffle(currentPlaylist)
                else -> currentPlaylist =
                    backupPlaylist.toMutableList()
            }
            songIndex = currentPlaylist.indexOf(thisSong)
        }
    }

    private fun playNew() = withCurrentSong {
        fetchUrl(it) { url ->
            logE("$$$", "get")
            playAction({
                mediaPlayer.playNew(url)
            }, {
                _isPlaying.value = true
                withCurrentSong {
                    logE("$$$", "lie")
                    updateRemoteViews(it)
                    listener?.onPlayingStateChanged(it, true)
                }
                startProgressJob()
            })
        }
    }

    private fun fetchUrl(song: Song, callback: ((String) -> Unit)? = null) {
        val s = songMap[song]

        logE("$$$", s.toString())

        if (s != null)
            callback?.invoke(s)
        else {
            loadJob?.cancel()
            loadJob = musicScope.launch {
                playerModel.getSongInfoExecute(song.id, "jymaster", appCookie,
                    {
                        try {
                            val url = it.data[0].url
                            songMap[song] = url

                            logE("$$$", url)

                            if (_song.value == song) {
                                logE("$$$", "pl")
                                callback?.invoke(url)
                            }
                        } catch (_: Exception) {
                        } finally {
                        }
                    },
                    { _, _ ->
                    })
            }
        }
    }

    private fun play() = playAction({
        if (currentPlaylist.isEmpty()) throw Exception("empty list") // 此处抛出异常是不希望进入 on success 块
        mediaPlayer.start()
    }, {
        _isPlaying.value = true
        withCurrentSong {
            logE("$$$", "lie")
            updateRemoteViews(it)
            listener?.onPlayingStateChanged(it, true)
        }
        startProgressJob()
    })

    private fun pause() = playAction({
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        mediaPlayer.pause()
    }, {
        _isPlaying.value = false
        withCurrentSong {
            updateRemoteViews(it)
            listener?.onPlayingStateChanged(it, false)
        }
    })

    /**
     * 播放下一曲
     */
    private fun next() = playAction({
        if (currentPlaylist.isEmpty()) throw Exception("empty list")

        songIndex++
        if (songIndex > currentPlaylist.size - 1) songIndex = 0
        playNew()
    }, {
        withCurrentSong {
            updateRemoteViews(it)
            listener?.onPlayingStateChanged(it, true)
        }
    })

    /**
     * 主动播放并加载下一曲
     */
    private fun previous() = playAction({
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        songIndex--
        if (songIndex < 0) songIndex =
            currentPlaylist.size - 1
        playNew()
    }, {
        withCurrentSong {
            updateRemoteViews(it)
            listener?.onPlayingStateChanged(it, true)
        }
    })

    private fun seekToPosition(position: Int) = playAction({
        val seekToPosition = (position * mediaPlayer.duration) / 100
        mediaPlayer.seekTo(seekToPosition)
    }, {
    })

    private fun changePlayMode() {
        if (_playMode.value == null) return
        _playMode.value = _playMode.value!! + 1
        if (_playMode.value!! > RANDOM)
            _playMode.value = LOOP

        formatPlaylists()
        listener?.onPlayModeChanged(_playMode.value!!)
    }


    /**
     * 用协程定时更新进度条
     */
    private fun startProgressJob() {
        progressJob?.cancel()

        progressJob = musicScope.launch {
            runCatching {
                while (isActive) {
                    delay(500)
                    if (!mediaPlayer.isPlaying) continue
                    val l = mediaPlayer.duration // 音频总长度
                    val p = mediaPlayer.currentPosition // 播放到的位置

                    val progress = (p * 100) / l // 得到播放进度的百分比
                    listener?.onProgressUpdated(progress)
                }
            }.onFailure { cancel() }
        }
    }

    inner class Player : MediaPlayer() {
        // player state
        private val INIT = 0
        private val CLEARED = 1
        private val SET = 2
        private val PREPARED = 3

        private var state = INIT

        init {
            clear()
        }

        fun clear() {
            if (state != INIT)
                clean()
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnCompletionListener {
                _isPlaying.value = false
                listener?.onProgressUpdated(0)

                if (_playMode.value == SINGLE)
                    play()
                else
                    next()
            }

            setOnErrorListener { _, _, _ ->
                clean()
                true
            }
        }

        private fun clean() = runCatching {
            seekTo(0)
            stop()
            reset()
            release()
            _isPlaying.value = false
            listener?.onProgressUpdated(0)
            state = CLEARED
        }.onFailure { e ->
            logE("$$$", e.message.toString())
        }

        override fun start() {
            logE("$$$", "stared")
            when (state) {
                SET -> {
                    prepare()
                    super.start()
                }

                PREPARED -> super.start()

                else -> playNew()
            }
        }

        override fun pause() {
            if (state == PREPARED)
                super.pause()
        }

        override fun prepare() {
            state = PREPARED
            logE("$$$", "prepared")
            super.prepare()
        }

        override fun seekTo(msec: Int) {
            if (state == PREPARED)
                super.seekTo(msec)
        }

        override fun setDataSource(path: String?) {
            super.setDataSource(path)
            state = SET
        }

        fun playNew(url: String) {
            clear()
            setDataSource(url)
            prepare()
            start()
        }
    }

    inner class MusicServiceBinder : Binder() {
        fun resetPlaylist(list: List<Song>) = this@MusicService2.updatePlaylist(list)
        fun setListener(l: MusicServiceListener2?) = this@MusicService2.setListener(l)

        fun previous() = this@MusicService2.previous()
        fun next() = this@MusicService2.next()
        fun seekToPosition(position: Int) = this@MusicService2.seekToPosition(position)
        fun changePlayMode() = this@MusicService2.changePlayMode()
        fun play() {
            if (_isPlaying.value == true)
                this@MusicService2.pause()
            else
                this@MusicService2.play()
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
        val iSwitch = Intent(this, MusicService2::class.java)
        iSwitch.action = ACTION_SWITCH
        val iPrevious = Intent(this, MusicService2::class.java)
        iPrevious.action = ACTION_PREVIOUS
        val iNext = Intent(this, MusicService2::class.java)
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
        updatePlayingStatus(_isPlaying.value!!)
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
