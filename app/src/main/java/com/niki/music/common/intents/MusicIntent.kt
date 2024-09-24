package com.niki.music.common.intents

import com.niki.music.dataclasses.Song
import com.niki.music.dataclasses.Sub

sealed class MusicIntent {
    class SetNewSongList(val list: MutableList<Song>, val index: Int) : MusicIntent()
    data object GetCatePlaylists : MusicIntent()
    class GetSongsFromPlaylist(
        val id: String,
        val limit: Int,
        val page: Int,
    ) : MusicIntent()

    class TryPlaySong(val songId: String) : MusicIntent()
}

sealed class MusicEffect {
    data class GetCatePlaylistsOkEffect(val subList: List<Sub>) : MusicEffect()
    data object GetCatePlaylistsBadEffect : MusicEffect()
    data class GetSongsFromPlaylistOkEffect(val songList: List<Song>) : MusicEffect()
    data object GetSongsFromPlaylistBadEffect : MusicEffect()
    data class TryPlaySongOkEffect(val url: String) : MusicEffect()
    data class TryPlaySongBadEffect(val msg: String) : MusicEffect()
}

data object MusicState