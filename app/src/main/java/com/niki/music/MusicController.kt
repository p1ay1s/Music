package com.niki.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.utils.shuffle
import com.niki.common.values.BroadCastMsg
import com.niki.music.models.PlayerModel
import com.niki.music.my.appCookie
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface MusicControllerListener {
    fun onSwitchedToNext(song: Song)
    fun onSwitchedToPrevious(song: Song)
    fun onSongPaused()
    fun onSongPlayed(song: Song)
    fun onProgressUpdated(newProgress: Int)
    fun onPlayModeChanged(newState: Int)
}

object MusicController {
    // player state
    private const val RELEASED = 0
    private const val INIT = 1
    private const val PREPARED = 2

    // play mode
    const val LOOP = 0
    const val SINGLE = 1
    const val RANDOM = 2

    private var _isPlaying = false
    val isPlaying
        get() = _isPlaying

    private var _playMode = LOOP
    val playMode
        get() = _playMode


    private val playerModel by lazy { PlayerModel() }
    private val musicScope by lazy { CoroutineScope(Dispatchers.IO) }
    private var progressJob: Job? = null
    private var loadMusicJob: Job? = null

    private var mainPlayer = Player()

    var listener: MusicControllerListener? = null
    private val receiver = RemoteControlReceiver()

    private var currentPlaylist: MutableList<Song> = mutableListOf() // TODO 本地化
    private var backupPlaylist: MutableList<Song> = mutableListOf() // 随机序列之前备份

    private var songIndex = 0

    init {
        mainPlayer.init()
    }

    private fun getCurrentSong() = currentPlaylist[songIndex]

    /**
     * 在需要立即播放时调用 , 加载并播放 , 成功时回调
     */
    private fun loadAndPlay() = runCatching {
        mainPlayer.clean()
        mainPlayer = Player()
        mainPlayer.prepareAndPlay(getCurrentSong())
    }.onSuccess {
        listener?.onSongPlayed(getCurrentSong())
        startProgressJob()
    }

    fun resetPlaylist(list: List<Song>) {
        songIndex = 0
        currentPlaylist = list.toMutableList()
        backupPlaylist = list.toMutableList()
        handlePlaylists()
        loadAndPlay()
    }

    fun play() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        mainPlayer.start()
    }.onSuccess {
        listener?.onSongPlayed(getCurrentSong())
        startProgressJob()
    }

    fun pause() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        mainPlayer.pause()
    }.onSuccess {
        listener?.onSongPaused()
    }

    fun seekToPosition(position: Int) = runCatching {
        val seekToPosition = (position * mainPlayer.duration) / 100
        mainPlayer.seekTo(seekToPosition)
    }.onFailure {
        listener?.onProgressUpdated(0)
    }

    fun changePlayMode() {
        _playMode++
        if (_playMode > RANDOM)
            _playMode = LOOP
        handlePlaylists()
        listener?.onPlayModeChanged(playMode)
    }

    private fun handlePlaylists() {
        if (currentPlaylist.isNotEmpty()) {
            val thisSong = currentPlaylist[songIndex] // 为了让索引重新指向当前播放的音乐
            when (playMode) {
                RANDOM -> shuffle(currentPlaylist)
                else -> currentPlaylist = backupPlaylist.toMutableList()
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
            }
        }
    }

    /**
     * 播放下一曲
     */
    fun next() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        songIndex++
        if (songIndex > currentPlaylist.size - 1) songIndex = 0
        loadAndPlay()
    }.onSuccess {
        listener?.onSwitchedToNext(getCurrentSong())
    }

    /**
     * 主动播放并加载下一曲
     */
    fun previous() = runCatching {
        if (currentPlaylist.isEmpty()) throw Exception("empty list")
        songIndex--
        if (songIndex < 0) songIndex = currentPlaylist.size - 1
        loadAndPlay()
    }.onSuccess {
        listener?.onSwitchedToPrevious(getCurrentSong())
    }

    fun MainActivity.releaseMusicController() = runCatching {
        this@MusicController.listener = null
        unregisterReceiver(receiver)
        progressJob?.cancel()
        progressJob = null
    }

    fun MainActivity.registerMusicReceiver() = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            BroadCastMsg.run {
                val filter = IntentFilter().apply {
                    addAction(PLAY)
                    addAction(PAUSE)
                    addAction(NEXT)
                    addAction(PREVIOUS)
                }
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED) // 接受服务的广播需要 exported
            }
    }

    class Player : MediaPlayer() {
        private var playerState = RELEASED

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
                    listener?.onProgressUpdated(0)
                    listener?.onSongPaused()

                    when (playMode) {
                        SINGLE -> play()
                        else -> next()
                    }
                }

                setOnErrorListener { _, _, _ ->
                    listener?.onProgressUpdated(0)
                    listener?.onSongPaused()
                    clean()
                    true
                }
                playerState = INIT
                listener?.onProgressUpdated(0)
                _isPlaying = false
            }
        }

        override fun start() {
            musicScope.launch {
                super.start()
                _isPlaying = true
            }
        }

        override fun pause() {
            super.pause()
            _isPlaying = false
        }

        fun clean() = runCatching {
            stop()
            reset()
            release()
            playerState = RELEASED
            _isPlaying = false
        }

        fun prepareAndPlay(song: Song) {
                loadMusicJob?.cancel()
//                loadMusicJob?.join()
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
                                } catch (e: Exception) {
                                    // 空指针
                                    e.printStackTrace()
                                    "播放失败".toast()
                                }
                            else
                                logE("####","dead")
                        },
                        { _, _ ->
                            if (isActive)
                                "播放失败".toast()
                        })
            }
        }
    }

    // 接受来自 remote views 的广播
    private class RemoteControlReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            BroadCastMsg.run {
                when (intent.action) {
                    PLAY -> play()
                    PAUSE -> pause()
                    NEXT -> next()
                    PREVIOUS -> previous()

                    else -> {}
                }
            }
        }
    }
}