package com.niki.common.repository.dataclasses.playlist.topPlaylistApi

import com.niki.common.repository.dataclasses.playlist.Playlist

data class TopPlaylistResponse(
    var more: Boolean,
    var code: Int,
    var playlists: List<Playlist> = listOf()
)

