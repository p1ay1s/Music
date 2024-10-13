package com.niki.music.common.viewModels

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.FragmentTag
import com.niki.music.RemoteControlService
import com.niki.music.intents.MainEffect
import com.niki.music.intents.MainIntent
import com.niki.music.intents.MainState
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.my.appCookie
import com.niki.music.search.preview.PreviewFragment
import com.p1ay1s.base.ui.FragmentHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel : BaseViewModel<MainIntent, MainState, MainEffect>() {
    companion object {
        const val SINGLE = 0
        const val LOOP = 1
        const val RANDOM = 2
    }

    private var job: Job? = null

    var binder: RemoteControlService.RemoteControlBinder? = null

    // 关于 current
    var currentPlaylist = mutableListOf<Song>()
    private var currentSong = MutableLiveData<Song?>(null)
    private var currentSongIndex = 0
    var isPlaying = MutableLiveData<Boolean>()
    var songPosition = MutableLiveData<Int>()
    var playMode = MutableLiveData(LOOP)

    var fragmentHost: FragmentHost? = null // 保存 fragment 的状态
    val fragmentMap: LinkedHashMap<Int, Class<out Fragment>> by lazy {
        linkedMapOf(
            FragmentTag.LISTEN_FRAGMENT to ListenFragment::class.java,
            FragmentTag.MY_FRAGMENT to MyFragment::class.java,
            FragmentTag.PREVIEW_FRAGMENT to PreviewFragment::class.java
        )
    }

    override fun initUiState() =
        MainState

    override fun handleIntent(intent: MainIntent) {
        intent.run {
            when (this) {
                is MainIntent.GetCatePlaylists -> {

                }

                is MainIntent.GetSongsFromPlaylist -> getSongsFromPlaylist(
                    id,
                    limit,
                    page,
                )

                is MainIntent.SetNewSongList -> setNewSongList(list, index)
                is MainIntent.TryPlaySong -> tryGetSongUrl(song)
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

    private fun getSongsFromPlaylist(
        id: String,
        limit: Int,
        page: Int,
    ) = playerModel.getSongsFromPlaylist(id, limit, page,
        { sendEffect { MainEffect.GetSongsFromPlaylistOkEffect(it.songs) } },
        { _, _ -> sendEffect { MainEffect.GetSongsFromPlaylistBadEffect } })

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
     * Effect-only
     */
    private fun tryGetSongUrl(song: Song) = viewModelScope.launch {
        job?.cancel()
        job?.join()
        job = launch(Dispatchers.IO) Job@{ // 加标签解决 scope 重名冲突问题
//            playerModel.checkSongAbility(song.id,
//                {
//                    if (it.success) { // 呃呃呃 不知道为啥不同的机子会请求到不同的结果 干脆如果是 200 不检查了
            val cookie = appCookie

            playerModel.getSongInfo(song.id, "jymaster", cookie,
                {
                    sendEffect { MainEffect.TryPlaySongOkEffect(it.data[0].url, song) }
                },
                { _, _ ->
                    sendEffect { MainEffect.TryPlaySongBadEffect("无法播放") }
                })
//                    } else {
//                        sendEffect { MainEffect.TryPlaySongBadEffect(it.message) }
//                    }
//                },
//                { _, _ ->
//                    sendEffect { MainEffect.TryPlaySongBadEffect("无法播放") }
//                })
        }
    }
}