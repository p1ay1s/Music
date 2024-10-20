package com.niki.music.viewModel

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import com.niki.common.repository.dataclasses.album.AlbumResponse
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.FragmentTag
import com.niki.music.listen.ListenFragment
import com.niki.music.my.MyFragment
import com.niki.music.search.ResultFragment
import com.p1ay1s.base.ui.FragmentHost

data object Null

class MainViewModel : BaseViewModel<Null, Null, Null>() {

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

    fun getSongsFromAlbum(
        id: String,
        callback: (songList: List<Song>?, album: AlbumResponse?) -> Unit
    ) {
        playerModel.getAlbumSongs(id,
            { response ->
                runCatching {
                    val ids = mutableListOf<String>()
                    response.songs.forEach {
                        ids.add(it.id)
                    }
                    getSongsWithIds(ids) {
                        if (!it.isNullOrEmpty())
                            callback(it, response)
                        else
                            callback(null, null)
                    }
                }.onFailure {
                    callback(null, null)
                }
            }, { _, _ ->
                callback(null, null)
            })
    }


    override fun handleIntent(intent: Null) = Unit

    override fun initUiState() = Null
}