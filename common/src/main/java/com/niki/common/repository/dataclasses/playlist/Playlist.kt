package com.niki.common.repository.dataclasses.playlist

data class Playlist(
    var name: String = "",
    var id: String = "",
    var coverImgUrl: String = "",
    var description: String = "",
    var tags: List<String> = listOf(),
    var creator: Creator = Creator()
)