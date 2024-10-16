package com.niki.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.BroadCastMsg
import com.niki.music.models.PlayerModel
import com.niki.music.my.appCookie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

interface MusicControllerListener {
    fun onSwitchedToNext(song: Song)
    fun onSwitchedToPrevious(song: Song)
    fun onSongPaused()
    fun onSongPlayed(song: Song)
    fun onProgressUpdated(new: Int)
}


object MusicController {

    private const val RELEASED = 0
    private const val INIT = 1
    private const val PREPARED = 2

    private val playerModel by lazy { PlayerModel() }
    private val musicScope by lazy { CoroutineScope(Dispatchers.IO) }

    private var mainPlayer = Player()

    var listener: MusicControllerListener? = null
    private val receiver = RemoteControlReceiver()

    private var currentPlaylist: List<Song> = emptyList() // TODO 本地化
    private var songIndex = 0

    private var thisSongUrl: String? = null

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
    }

    fun resetPlaylist(list: List<Song>) {
        songIndex = 0
        currentPlaylist = list
        loadAndPlay()
    }

    fun play() = runCatching {
        mainPlayer.start()
    }.onSuccess {
        listener?.onSongPlayed(getCurrentSong())
    }

    fun pause() = runCatching {
        mainPlayer.pause()
    }.onSuccess {
        listener?.onSongPaused()
    }

    /**
     * 播放下一曲
     */
    fun next() = runCatching {
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
        songIndex--
        if (songIndex < 0) songIndex = currentPlaylist.size - 1
        loadAndPlay()
    }.onFailure {
        listener?.onSwitchedToPrevious(getCurrentSong())
    }

    fun MainActivity.releaseMusicController() = runCatching {
        this@MusicController.listener = null
        unregisterReceiver(receiver)
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
                    listener?.onSongPaused()
                    next()
                }

                setOnErrorListener { _, _, _ ->
                    listener?.onSongPaused()
                    clean()
                    true
                }
                playerState = INIT
            }
        }

        fun clean() = runCatching {
            stop()
            reset()
            release()
            playerState = RELEASED
        }

        fun prepareAndPlay(song: Song) {
            playerModel.getSongInfo(song.id, "jymaster", appCookie,
                {
                    runCatching {
                        val url = it.data[0].url
                        if (url != null) {
                            init()
                            setDataSource(it.data[0].url)
                            prepare()
                            playerState = PREPARED
                            start()
                        } else {
                            next()
                        }
                    }
                },
                { _, _ ->
                })
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