package com.niki.common.repository.dataclasses.album

data class AlbumResponse(
    val album: AlbumDetails,
    val code: Int,
    val resourceState: Boolean,
    val songs: List<AlbumSong>
)