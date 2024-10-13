package com.niki.music.intents

import com.niki.common.repository.dataclasses.Song
import com.niki.common.repository.dataclasses.Sub

sealed class MainIntent {
    class SetNewSongList(val list: MutableList<Song>, val index: Int) : MainIntent()
    data object GetCatePlaylists : MainIntent()
    class GetSongsFromPlaylist(
        val id: String,
        val limit: Int,
        val page: Int,
    ) : MainIntent()

    class TryPlaySong(val song: Song) : MainIntent()
}

sealed class MainEffect {
    data class GetCatePlaylistsOkEffect(val subList: List<Sub>) : MainEffect()
    data object GetCatePlaylistsBadEffect : MainEffect()
    data class GetSongsFromPlaylistOkEffect(val songList: List<Song>) : MainEffect()
    data object GetSongsFromPlaylistBadEffect : MainEffect()
    data class TryPlaySongOkEffect(val url: String, val song: Song) : MainEffect()
    data class TryPlaySongBadEffect(val msg: String) : MainEffect()
}

data object MainState