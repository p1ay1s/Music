package com.niki.common.repository.dataclasses.song

data class Song(
    var name: String = "",
    var id: String = "",
    var ar: List<Author> = listOf(),
    var al: Album = Album(),
    var dt: Int = 0, // 歌曲长度
    var mark: Long = 0 // 1048576脏标
)