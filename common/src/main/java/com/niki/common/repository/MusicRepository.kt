package com.niki.common.repository

import com.niki.common.repository.dataclasses.Playlist
import com.niki.common.repository.dataclasses.Song
import com.niki.common.repository.dataclasses.Tag

object MusicRepository {
    // 关于 top
    var topPlaylists = listOf<Playlist>()

    // 关于 hot
    var hotPlaylists = listOf<Tag>()

    // 关于 search
    var searchPlaylist = listOf<Song>()

    // 关于 like
    var likePlaylist = listOf<Song>()
}