package com.niki.music.search

sealed class SearchIntent {
    data class KeywordsChanged(val keywords: String) : SearchIntent()
    data object CleanAll : SearchIntent()
    data object SearchSongs : SearchIntent()
}

sealed class SearchEffect {
    data class SearchSongsState(val isSuccess: Boolean = false) : SearchEffect()
}

data class SearchState(
    val searchContent: String,
    val searchHasMore: Boolean,
    val searchCurrentPage: Int,
)