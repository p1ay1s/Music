package com.niki.common.repository.dataclasses.song.getSongInfoApi

import com.niki.common.repository.dataclasses.song.SongInfo

data class GetSongInfoApiResponse(
    var code: Int,
    var data: List<SongInfo> = listOf()
)
