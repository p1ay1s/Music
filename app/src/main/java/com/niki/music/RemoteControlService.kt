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
import com.niki.music.models.PlayerModel
import com.niki.music.my.appCookie
import com.p1ay1s.base.log.logD
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface MusicServiceListener {
    fun onPlayingStateChanged(song: Song, isPlaying: Boolean)
    fun onProgressUpdated(newProgress: Int)
    fun onPlayModeChanged(newState: Int)
}

class RemoteControlService : Service() {
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

    private lateinit var remoteViews: RemoteViews
    private val binder = RemoteControlBinder()
    private var listener: MusicServiceListener? = null

    private var _isPlaying = MutableLiveData(false)
        get() {
            logE("####", field.value.toString())
            return field
        }

    private var _playMode = MutableLiveData(LOOP)

    private val playerModel by lazy { PlayerModel() }
    private val musicScope by lazy { CoroutineScope(Dispatchers.IO) }
    private var progressJob: Job? = null
    private var loadMusicJob: Job? = null

    private var mainPlayer = Player()

    private var currentPlaylist: MutableList<Song> = mutableListOf()
    private var backupPlaylist: MutableList<Song> = mutableListOf() // 随机序列之前备份

    private var songIndex = 0

    init {
        _isPlaying.observeForever { isPlaying ->
            setPlayingStatus(isPlaying)
        }
        _playMode.observeForever {
            listener?.onPlayModeChanged(it)
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
        }
    }

    private fun createNotification(): Notification {
        val iSwitch = Intent(this, RemoteControlService::class.java)
        iSwitch.action = ACTION_SWITCH
        val iPrevious = Intent(this, RemoteControlService::class.java)
        iPrevious.action = ACTION_PREVIOUS
        val iNext = Intent(this, RemoteControlService::class.java)
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
            "music control channel",
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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.action) {
            ACTION_SWITCH -> {
                if (_isPlaying.value!!)
                    pause()
                else
                    play()
            }

            ACTION_PREVIOUS -> {
                previous()
            }

            ACTION_NEXT -> {
                next()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    /**
     * 不要手动调用 , 使用 changeSong
     */
    private fun setSong(song: Song, callback: () -> Unit) {
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
        setCover(song.al.picUrl, callback)
    }

    /**
     * 不要手动调用 , 使用 changeSong
     */
    private fun setCover(
        imageUrl: String,
        callback: () -> Unit
    ) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .override(256)
            .transform(RoundedCorners(35))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
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

    private fun changeSong(song: Song) {
        if (!::remoteViews.isInitialized) return
        setSong(song) {
            refreshRemoteView()
        }
    }

    private fun setPlayingStatus(isPlaying: Boolean) {
        if (!::remoteViews.isInitialized) return
        if (isPlaying)
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_pause)
        else
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_play)
        refreshRemoteView()
    }

    /**
     * 不要手动调用
     */
    private fun refreshRemoteView() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    inner class RemoteControlBinder : Binder() {
        fun resetPlaylist(list: List<Song>) = this@RemoteControlService.resetPlaylist(list)
        fun play() {
            if (_isPlaying.value == null) return

            if (_isPlaying.value!!)
                this@RemoteControlService.pause()
            else
                this@RemoteControlService.play()
        }

        fun previous() = this@RemoteControlService.previous()
        fun next() = this@RemoteControlService.next()
        fun seekToPosition(position: Int) = this@RemoteControlService.seekToPosition(position)
        fun setListener(l: MusicServiceListener?) = this@RemoteControlService.setListener(l)
        fun changePlayMode() = this@RemoteControlService.changePlayMode()
    }

    private fun setListener(l: MusicServiceListener?) {
        listener = l
    }

    /**
     * 只要出现错误就修复
     */
    private fun reset() {
        mainPlayer.clean()
        mainPlayer = Player()
    }

    private inline fun withCurrentSong(crossinline callback: (Song) -> Unit) = try {
        val song = currentPlaylist[songIndex]
        callback(song)
    } catch (_: Exception) {
    }

    private inline fun playAction(
        crossinline action: () -> Unit,
        crossinline onSuccess: () -> Unit
    ) = runCatching {
        action()
    }.onFailure {
        mainPlayer.playerState = ERROR
        _isPlaying.value = false
        reset()
    }.onSuccess {
        onSuccess()
    }

    private fun loadAndPlay() = playAction({
        reset()
        withCurrentSong { mainPlayer.prepareAndPlay(it) }
    }, {
        startProgressJob()
        withCurrentSong {
            changeSong(it)
            listener?.onPlayingStateChanged(it, true)
        }
    })

    fun resetPlaylist(list: List<Song>) {
        songIndex = 0
        currentPlaylist = list.toMutableList()
        backupPlaylist = list.toMutableList()
        handlePlaylists()
        loadAndPlay()
    }

    private fun play() = playAction({
        if (currentPlaylist.isEmpty()) throw Exception("empty list") // 此处抛出异常是不希望进入 on success 块
        mainPlayer.start()
    }, {
        startProgressJob()
        withCurrentSong {
            changeSong(it)
            listener?.onPlayingStateChanged(it, true)
        }
    })

    private fun pause() = playAction({
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        mainPlayer.pause()
    }, {
        withCurrentSong {
            changeSong(it)
            listener?.onPlayingStateChanged(it, false)
        }
    })

    private fun seekToPosition(position: Int) = playAction({
        val seekToPosition = (position * mainPlayer.duration) / 100
        mainPlayer.seekTo(seekToPosition)
    }, {
    })

    private fun changePlayMode() {
        if (_playMode.value == null) return
        _playMode.value = _playMode.value!! + 1
        if (_playMode.value!! > RANDOM)
            _playMode.value = LOOP

        handlePlaylists()
        listener?.onPlayModeChanged(_playMode.value!!)
    }

    private fun handlePlaylists() {
        if (currentPlaylist.isNotEmpty()) {
            val thisSong =
                currentPlaylist[songIndex] // 为了让索引重新指向当前播放的音乐
            when (_playMode.value) {
                RANDOM -> shuffle(currentPlaylist)
                else -> currentPlaylist =
                    backupPlaylist.toMutableList()
            }
            songIndex = currentPlaylist.indexOf(thisSong)
        }
    }

    private fun startProgressJob() {
        progressJob?.cancel()

        progressJob = musicScope.launch {
            runCatching {
                while (isActive) {
                    delay(500)
                    if (!mainPlayer.isPlaying) continue
                    val l = mainPlayer.duration // 音频总长度
                    val p = mainPlayer.currentPosition // 播放到的位置
                    val progress = (p * 100) / l // 得到播放进度的百分比
                    listener?.onProgressUpdated(progress)
                }
            }.onFailure { cancel() }
        }
    }

    /**
     * 播放下一曲
     */
    private fun next() = playAction({
        if (currentPlaylist.isEmpty()) throw Exception("empty list")

        songIndex++
        if (songIndex > currentPlaylist.size - 1) songIndex = 0
        loadAndPlay()
    }, {
        withCurrentSong {
            changeSong(it)
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
        loadAndPlay()
    }, {
        withCurrentSong {
            changeSong(it)
            listener?.onPlayingStateChanged(it, true)
        }
    })

    inner class Player : MediaPlayer() {
        var playerState = RELEASED

        init {
            init()
        }

        fun init() {
            runCatching {
                setAudioAttributes(
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnCompletionListener {
                    if (_playMode.value == SINGLE) {
                        play()
                    } else {
                        next()
                    }
                }

                setOnErrorListener { _, _, _ ->
                    playerState = ERROR
                    clean()
                    true
                }

                playerState = INIT
                _isPlaying.value = false
            }
        }

        override fun start() {
            if (playerState != PREPARED) throw Exception("not prepared")
            _isPlaying.value = true
            logD("####", "called successfully")
            super.start()
        }

        override fun pause() {
            if (playerState != PREPARED) throw Exception("not prepared")
            super.pause()
            _isPlaying.value = false
        }

        fun clean() = runCatching {
            stop()
            reset()
            release()
            playerState = RELEASED
            _isPlaying.value = false
        }

        override fun seekTo(msec: Int) {
            if (playerState != PREPARED) throw Exception("not prepared")
            super.seekTo(msec)
        }

        fun prepareAndPlay(song: Song) {
            loadMusicJob?.cancel()
            loadMusicJob = musicScope.launch {
                playerModel.getSongInfoExecute(song.id, "jymaster", appCookie,
                    {
                        if (isActive)
                            try {
                                val url = it.data[0].url
                                init()
                                setDataSource(url)
                                prepare()
                                playerState = PREPARED
                                start()
                                _isPlaying.value = true
                            } catch (e: Exception) {
                                // 空指针
                                playerState = ERROR
                                e.printStackTrace()
                            }
                    },
                    { _, _ ->
                        playerState = ERROR
                    })
            }
        }
    }
}
