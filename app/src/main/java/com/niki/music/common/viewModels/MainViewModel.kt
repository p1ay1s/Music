package com.niki.music.common.viewModels

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.niki.common.repository.dataclasses.Song
import com.niki.common.values.FragmentTag
import com.niki.music.intents.MusicEffect
import com.niki.music.intents.MusicIntent
import com.niki.music.intents.MusicState
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.appCookie
import com.niki.music.search.preview.PreviewFragment
import com.niki.music.search.result.ResultModel
import com.p1ay1s.base.ui.FragmentHost
import com.p1ay1s.util.ON_FAILURE_CODE

class MainViewModel : BaseViewModel<MusicIntent, MusicState, MusicEffect>() {
    companion object {
        const val SINGLE = 0
        const val LOOP = 1
        const val RANDOM = 2
    }

    private val resultModel by lazy { ResultModel() }

    // 关于 current
    var currentPlaylist = mutableListOf<Song>()
    private var currentSong = MutableLiveData<Song?>(null)
    private var currentSongIndex = 0
    var isPlaying = MutableLiveData<Boolean>()
    var songPosition = MutableLiveData<Int>()
    var playMode = MutableLiveData(LOOP)

    var fragmentHost: FragmentHost? = null // 保存 fragment 的状态
    val fragmentMap: LinkedHashMap<Int, Class<out Fragment>> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            linkedMapOf(
                FragmentTag.LISTEN_FRAGMENT to ListenFragment::class.java,
                FragmentTag.MY_FRAGMENT to MyFragment::class.java,
                FragmentTag.PREVIEW_FRAGMENT to PreviewFragment::class.java
            )
        } else {
            throw Exception("unsupported android version")
        }
    }

    override fun initUiState() =
        MusicState

    override fun handleIntent(intent: MusicIntent) {
        intent.run {
            when (this) {
                is MusicIntent.GetCatePlaylists -> getCatePlaylists()
                is MusicIntent.GetSongsFromPlaylist -> getSongsFromPlaylist(
                    id,
                    limit,
                    page,
                )

                is MusicIntent.SetNewSongList -> setNewSongList(list, index)
                is MusicIntent.TryPlaySong -> tryPlaySong(songId)
            }
        }
    }

//        fun addSongToNext(song: Song) {
//        if (currentPlayList.size <= 1)
//            currentPlayList.add(song)
//        else
//            currentPlayList.add(currentIndex + 1, song)
//
//    }

    private fun setNewSongList(list: MutableList<Song>, index: Int = 0) {
        currentPlaylist = list
        currentSongIndex = index
        currentSong.value = currentPlaylist[currentSongIndex]
    }

//    fun switchMode() {
//        when (playMode.value) {
//            RANDOM -> playMode.value = SINGLE
//            else -> playMode.value = playMode.value?.plus(1)
//        }
//    }


    /**
     * 之前写了没用过
     */
    private fun getCatePlaylists() =
        playlistModel.getCatePlaylists(
            {
                sendEffect { MusicEffect.GetCatePlaylistsOkEffect(it.sub) }
            },
            { _, _ ->
                sendEffect { MusicEffect.GetCatePlaylistsBadEffect }
            })

    private fun getSongsFromPlaylist(
        id: String,
        limit: Int,
        page: Int,
    ) = playerModel.getSongsFromPlaylist(id, limit, page,
        { sendEffect { MusicEffect.GetSongsFromPlaylistOkEffect(it.songs) } },
        { _, _ -> sendEffect { MusicEffect.GetSongsFromPlaylistBadEffect } })

    fun getSongsFromPlaylist(
        id: String,
        limit: Int,
        page: Int,
        callback: (songList: List<Song>?) -> Unit
    ) = playerModel.getSongsFromPlaylist(
        id,
        limit,
        page,
        { callback(it.songs) },
        { _, _ -> callback(null) })

    /**
     * TODO
     */
    private fun tryPlaySong(songId: String) {
        playerModel.checkSongAbility(songId,
            {
                if (it.success) {
                    val cookie = appCookie

                    playerModel.getSongInfo(songId, "jymaster", cookie,
                        {
                            sendEffect { MusicEffect.TryPlaySongOkEffect(it.data[0].url) }
                        },
                        { code, _ ->
                            sendEffect { MusicEffect.TryPlaySongBadEffect(if (code == ON_FAILURE_CODE) "" else "无法播放") }
                        })
                }
            },
            { code, _ ->
                sendEffect { MusicEffect.TryPlaySongBadEffect(if (code == ON_FAILURE_CODE) "" else "无法播放") }
            })
    }
}