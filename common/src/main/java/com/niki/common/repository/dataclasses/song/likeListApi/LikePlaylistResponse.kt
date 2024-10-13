package com.niki.common.repository.dataclasses.song.likeListApi

data class LikePlaylistResponse(
    val ids: List<String> = listOf(),
    var code: Int
)