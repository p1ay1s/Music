package com.niki.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.BroadCastMsg

interface MusicControllerListener {
    fun onSwitchToNext()
    fun onSwitchToPrevious()
    fun onSongPaused()
    fun onSongPlayed()
}

object MusicController {
    private const val RELEASED = 0
    private const val INIT = 1
    private const val PREPARED = 2

    private lateinit var mediaPlayer: MediaPlayer
    private var playerState = 0

    private var listener: MusicControllerListener? = null
    private val receiver = RemoteControlReceiver()

    private var currentPlaylist: List<Song> = emptyList() // TODO 本地化
    private var songIndex = 0
    private var nowPlaying: String? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            setOnCompletionListener {
                listener?.onSongPaused()
                next()
            }

            setOnErrorListener { _, _, _ ->
                listener?.onSongPaused()
                stop()
                true
            }
        }
        playerState = INIT
    }

    fun prepareSong(url: String) {
        if (playerState != RELEASED) stop()
        listener?.onSongPaused()
        nowPlaying = url
        initPlayer()
        mediaPlayer.apply {
            setDataSource(url)
            prepare()
        }
        playerState = PREPARED
    }

    fun resetPlaylist(list: List<Song>) {
        songIndex = 0
        currentPlaylist = list
    }

    fun setListener(l: MusicControllerListener) {
        this@MusicController.listener = l
    }

    fun play() = runCatching {
        if (playerState != PREPARED) throw Exception("PLAY->wrong state!")
        mediaPlayer.start()
    }.onSuccess {
        listener?.onSongPlayed()
    }

    fun pause() = runCatching {
        if (playerState != PREPARED) throw Exception("PAUSE->wrong state!")
        mediaPlayer.pause()
    }.onSuccess {
        listener?.onSongPaused()
    }

    private fun stop() {
        mediaPlayer.reset()
        mediaPlayer.release()
        playerState = RELEASED
    }

    fun next() {
        songIndex++
        if (songIndex > currentPlaylist.size - 1) songIndex = 0
        // TODO
        listener?.onSwitchToNext()
    }

    fun previous() {
        songIndex--
        if (songIndex < 0) songIndex = currentPlaylist.size - 1
        // TODO
        listener?.onSwitchToPrevious()
    }


    fun MainActivity.releaseMusicController() {
        this@MusicController.listener = null
        unregisterReceiver(receiver)
    }

    fun MainActivity.registerMusicReceiver() = runCatching {
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