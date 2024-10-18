package com.niki.music.common.viewModels

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.FragmentTag
import com.niki.music.intents.MainEffect
import com.niki.music.intents.MainIntent
import com.niki.music.intents.MainState
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.search.preview.PreviewFragment
import com.p1ay1s.base.ui.FragmentHost
import kotlinx.coroutines.Job

class MainViewModel : BaseViewModel<MainIntent, MainState, MainEffect>() {

    var playerBackground: Drawable? = null
    var currentSong: Song? = null

    var fragmentHost: FragmentHost? = null // 保存 fragment 的状态

    val fragmentMap: LinkedHashMap<Int, Class<out Fragment>> by lazy {
        linkedMapOf(
            FragmentTag.LISTEN_FRAGMENT to ListenFragment::class.java,
            FragmentTag.MY_FRAGMENT to MyFragment::class.java,
            FragmentTag.PREVIEW_FRAGMENT to PreviewFragment::class.java
        )
    }

    override fun initUiState() = MainState

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
            }
        }
    }

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
}