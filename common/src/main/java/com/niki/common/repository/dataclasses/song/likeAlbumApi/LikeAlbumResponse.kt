package com.niki.common.repository.dataclasses.song.likeAlbumApi

data class LikeAlbumResponse(
    val code: Int,
    val count: Int,
    val data: List<LikeAlbumData>,
    val hasMore: Boolean,
    val paidCount: Int
)