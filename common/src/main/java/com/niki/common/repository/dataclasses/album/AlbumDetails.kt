package com.niki.common.repository.dataclasses.album

data class AlbumDetails(
    val artist: AlbumArtist,
    val description: String,
    val name: String,
    val picUrl:String,
)