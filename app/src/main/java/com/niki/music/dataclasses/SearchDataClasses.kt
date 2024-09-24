package com.niki.music.dataclasses

data class SearchApiResponse(
    var result: SearchResult? = null,
    var code: Int
)

data class SearchResult(
    var songs: List<SongSearch>? = null,
    var songCount: Int,
    var hasMore: Boolean
)

data class SongSearch(
    var name: String? = "null",
    var id: String?,
    var artists: List<AuthorSearch>,
    var album: Album?,
    var duration: Int?, // 歌曲长度
)

data class AuthorSearch(
    var id: String,
    var name: String,
)