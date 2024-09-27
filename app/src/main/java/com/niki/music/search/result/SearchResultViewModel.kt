package com.niki.music.search.result

import com.niki.music.common.MusicRepository
import com.niki.music.common.viewModels.BaseViewModel
import com.niki.music.search.SearchModel
import com.p1ay1s.dev.base.TAG
import com.p1ay1s.dev.log.logE

class SearchResultViewModel : BaseViewModel<SearchIntent, SearchState, SearchEffect>() {
    private val searchModel by lazy { SearchModel() }

    companion object {
        const val SEARCH_LIMIT = 20
    }

    override fun initUiState() = SearchState("", true, 0)

    override fun handleIntent(intent: SearchIntent) =
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
            when (this) {
                is SearchIntent.SearchSongs -> {
                    uiStateFlow.value.run {
                        searchSongs(searchContent)
                    }
                }

                SearchIntent.CleanAll -> {
                    updateState {
                        copy(
                            searchHasMore = true,
                            searchCurrentPage = 0
                        )
                    }
                    MusicRepository.mSearchPlaylist = emptyList()
                    sendEffect { SearchEffect.SearchSongsState(true) }
                    Unit
                }

                is SearchIntent.KeywordsChanged -> updateState { copy(searchContent = keywords) }
            }
        }

    private fun searchSongs(keywords: String?) = uiStateFlow.value.run {
        if (keywords.isNullOrBlank() || !searchHasMore) return@run

        searchModel.searchSongs(keywords, SEARCH_LIMIT, searchCurrentPage * SEARCH_LIMIT,
            { data ->
                val list = mutableListOf<String>()
                for (song in data.result!!.songs!!)
                    list.add(song.id!!)
                getSongsWithIds(list) { songList ->
                    if (!songList.isNullOrEmpty()) {
                        MusicRepository.mSearchPlaylist =
                            MusicRepository.mSearchPlaylist.plus(songList)
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