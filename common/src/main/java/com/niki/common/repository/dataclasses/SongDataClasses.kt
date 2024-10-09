package com.niki.common.repository.dataclasses

/**
 * 从单个歌单获取的歌曲
 */
data class GetSongsFromPlaylistApiResponse(
    var songs: List<Song> = listOf(),
    var code: Int
)

data class Song(
    var name: String = "",
    var id: String = "",
    var ar: List<Author> = listOf(),
    var al: Album = Album(),
    var dt: Int = 0, // 歌曲长度
    var mark: Long = 0 // 1048576脏标
)

/**
 * 包含了所有收藏歌曲的歌曲 id
 */
data class LikePlaylistResponse(
    val ids: List<String> = listOf(),
    var code: Int
)

data class GetSongInfoApiResponse(
    var code: Int,
    var data: List<SongInfo> = listOf()
)

data class SongInfo(
    var code: Int,
    var url: String = ""
)

data class AvailableResponse(
    var success: Boolean = false,
    var code: Int
)

data class Author(
    var id: String = "",
    var name: String = "",
)

data class Album(
    var id: String = "",
    var name: String = "",
    var picUrl: String = ""
)