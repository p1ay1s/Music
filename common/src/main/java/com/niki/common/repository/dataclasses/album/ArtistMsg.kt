package com.niki.common.repository.dataclasses.album

data class ArtistMsg(
    val followed: Boolean,
    val id: Int,
    val name: String,
    val picUrl: String,
)