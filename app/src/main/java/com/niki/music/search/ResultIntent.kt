package com.niki.music.search

import com.niki.common.repository.dataclasses.song.Song

sealed class ResultIntent {
    data class KeywordsChanged(val keywords: String) : ResultIntent()
    data object SearchSongs : ResultIntent()
}

sealed class ResultEffect {
    data object KeywordsFailedEffect : ResultEffect()
    data object KeywordSuccessEffect : ResultEffect()
}

data class ResultState(
    val searchContent: String,
    val searchHasMore: Boolean,
    val searchCurrentPage: Int,
    val songList: List<Song>?,
    val idList: List<String>?,
    val hotKeywords: List<String>?,
    val suggestKeywords: List<String>?
)