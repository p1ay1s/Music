package com.niki.music.viewModel

import android.graphics.drawable.Drawable
import com.niki.common.repository.dataclasses.album.AlbumResponse
import com.niki.common.repository.dataclasses.playlist.Playlist
import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.ui.Host

data object Null

class MainViewModel : BaseViewModel<Null, Null, Null>() {

    companion object {
        const val MAX_PLAYLIST_SIZE = 10
    }

    var playerBackground: Drawable? = null
    var currentSong: Song? = null

    var hostMap: HashMap<Int, Host>? = null
    var host: Host? = null

    var activityIndex = -1

    var playlistMap: LimitedMap<String, Pair<Playlist, List<Song>>> = LimitedMap(MAX_PLAYLIST_SIZE)

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

    class LimitedMap<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    override fun handleIntent(intent: Null) = Unit

    override fun initUiState() = Null
}