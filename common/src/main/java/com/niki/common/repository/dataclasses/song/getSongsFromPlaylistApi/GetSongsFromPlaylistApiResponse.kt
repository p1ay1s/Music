package com.niki.common.repository.dataclasses.song.getSongsFromPlaylistApi

import com.niki.common.repository.dataclasses.song.Song

data class GetSongsFromPlaylistApiResponse(
    var songs: List<Song> = listOf(),
    var code: Int
)
