package com.niki.music.search

import com.niki.music.common.MusicRepository
import com.niki.music.common.commonViewModels.BaseViewModel

class SearchViewModel : BaseViewModel<SearchIntent, SearchState, SearchEffect>() {
    private val searchModel by lazy { SearchModel() }

    companion object {
        const val SEARCH_LIMIT = 20
    }

    override fun initUiState() = SearchState(true, 0)

    override fun handleIntent(intent: SearchIntent) =
        intent.run {
            when (this) {
                is SearchIntent.SearchSongs -> searchSongs(keywords, resetPage, clean)
                is SearchIntent.ResetSearchContent -> resetSearchContent()
            }
        }

    private fun resetSearchContent() =
        updateState { copy(searchHasMore = true, searchCurrentPage = 0) }

    private fun searchSongs(
        keywords: String?,
        resetPage: Boolean = false,
        clean: Boolean = false
    ) = uiStateFlow.value.run {
        if (keywords.isNullOrBlank() || !searchHasMore) return

        if (resetPage)
            resetSearchContent()

        searchModel.searchSongs(keywords, SEARCH_LIMIT, searchCurrentPage * SEARCH_LIMIT,
            { data ->
                val list = mutableListOf<String>()
                for (song in data.result!!.songs!!)
                    list.add(song.id!!)
                getSongsWithIds(list) { songList ->
                    if (!songList.isNullOrEmpty()) {
                        MusicRepository.mSearchPlaylist =
                            if (clean) songList else MusicRepository.mSearchPlaylist.plus(songList)
                        updateState {
                            copy(
                                searchHasMore = data.result!!.hasMore,
                                searchCurrentPage = searchCurrentPage + 1,
                            )
                        }
                        sendEffect { SearchEffect.SearchSongsState(true) }
                    } else
                        sendEffect { SearchEffect.SearchSongsState() }
                }
            },
            { _, _ -> sendEffect { SearchEffect.SearchSongsState() } })
    }

}