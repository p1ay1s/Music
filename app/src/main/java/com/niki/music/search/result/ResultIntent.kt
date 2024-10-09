package com.niki.music.search.result

sealed class ResultIntent {
    data class KeywordsChanged(val keywords: String) : ResultIntent()
    data object SearchSongs : ResultIntent()
}

sealed class ResultEffect {
    data class SearchSongsState(val isSuccess: Boolean = false) : ResultEffect()
}

data class ResultState(
    val searchContent: String,
    val searchHasMore: Boolean,
    val searchCurrentPage: Int,
)