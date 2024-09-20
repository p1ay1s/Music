package com.niki.music.common.viewModels

import androidx.lifecycle.MutableLiveData
import com.niki.music.common.intents.MusicEffect
import com.niki.music.common.intents.MusicIntent
import com.niki.music.common.intents.MusicState
import com.niki.music.model.Song
import com.niki.music.my.appCookie
import com.niki.music.search.SearchModel
import com.niki.base.util.ON_FAILURE_CODE

class MusicViewModel : BaseViewModel<MusicIntent, MusicState, MusicEffect>() {
    companion object {
        const val SINGLE = 0
        const val LOOP = 1
        const val RANDOM = 2
    }

    private val searchModel by lazy { SearchModel() }

    // 关于 current
    var mCurrentPlaylist = mutableListOf<Song>()
    private var mCurrentSong = MutableLiveData<Song?>(null)
    private var mCurrentSongIndex = 0
    var mIsPlaying = MutableLiveData<Boolean>()
    var mSongPosition = MutableLiveData<Int>()
    var mPlayMode = MutableLiveData(LOOP)

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
        mCurrentPlaylist = list
        mCurrentSongIndex = index
        mCurrentSong.value = mCurrentPlaylist[mCurrentSongIndex]
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