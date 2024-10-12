package com.niki.music.search.result

import com.niki.common.repository.dataclasses.Song

sealed class ResultIntent {
    data class KeywordsChanged(val keywords: String) : ResultIntent()
    data object SearchSongs : ResultIntent()
}

sealed class ResultEffect {
}

data class ResultState(
    val searchContent: String,
    val searchHasMore: Boolean,
    val searchCurrentPage: Int,
    val songList: List<Song>?
)