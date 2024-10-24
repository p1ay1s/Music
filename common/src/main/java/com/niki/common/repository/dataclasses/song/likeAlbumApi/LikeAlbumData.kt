package com.niki.common.repository.dataclasses.song.likeAlbumApi

import com.niki.common.repository.dataclasses.album.ArtistMsg

data class LikeAlbumData(
    val artists: List<ArtistMsg>,
    val id: Int,
    val name: String,
    val picUrl: String,
    val size: Int,
    val subTime: Long, // 排序
)