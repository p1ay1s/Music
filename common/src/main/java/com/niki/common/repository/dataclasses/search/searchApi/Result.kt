package com.niki.common.repository.dataclasses.search.searchApi

import com.niki.common.repository.dataclasses.Song

data class Result(
    val hasMore: Boolean,
    val songCount: Int,
    val songs: List<Song>
)