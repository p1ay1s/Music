package com.niki.music.viewModel

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.FragmentTag
import com.niki.music.listen.ListenFragment
import com.niki.music.model.PlayerModel
import com.niki.music.my.MyFragment
import com.niki.music.search.ResultFragment
import com.p1ay1s.base.ui.FragmentHost

class MainViewModel : ViewModel() {
    private val playerModel by lazy { PlayerModel() }

    var playerBackground: Drawable? = null
    var currentSong: Song? = null

    var fragmentHost: FragmentHost? = null // 保存 fragment 的状态

    val fragmentMap: LinkedHashMap<Int, Class<out Fragment>> by lazy {
        linkedMapOf(
            FragmentTag.LISTEN_FRAGMENT to ListenFragment::class.java,
            FragmentTag.MY_FRAGMENT to MyFragment::class.java,
            FragmentTag.RESULT_FRAGMENT to ResultFragment::class.java
        )
    }

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