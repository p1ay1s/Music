package com.niki.music.search

sealed class SearchIntent {
    class SearchSongs(val keywords: String?, val resetPage: Boolean, val clean: Boolean) :
        SearchIntent()

    data object ResetSearchContent : SearchIntent()
}

sealed class SearchEffect {
    data class SearchSongsState(val isSuccess: Boolean = false) : SearchEffect()
}

data class SearchState(
    val searchHasMore: Boolean,
    val searchCurrentPage: Int,
)