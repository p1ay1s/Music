package com.niki.music.search.result

import com.niki.common.repository.dataclasses.search.searchApi.SearchResponse
import com.niki.music.common.viewModels.BaseViewModel
import com.p1ay1s.base.extension.TAG
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.log.logE

class ResultViewModel : BaseViewModel<ResultIntent, ResultState, ResultEffect>() {
    private val resultModel by lazy { ResultModel() }

    companion object {
        const val SEARCH_LIMIT = 20
    }

    private var isSearching = false
    private var hotIsLoading = false
    private var suggestIsLoading = false

    override fun initUiState() = ResultState("", true, 0, null, null, null, null)

    init {
        getHotSuggest()
    }

    override fun handleIntent(intent: ResultIntent) =
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
            when (this) {
                is ResultIntent.SearchSongs -> searchSongs()

                is ResultIntent.KeywordsChanged ->
                    if (state.searchContent != keywords) {
                        updateState { copy(searchContent = keywords) }
                        resetResult()
                        getRelativeSuggest()
                    }
            }
        }

    private fun resetResult() {
        updateState {
            copy(
                searchCurrentPage = 0,
                searchHasMore = true,
                songList = null,
                idList = null,
                suggestKeywords = null
            )
        }
    }

    private fun getHotSuggest() {
        if (hotIsLoading) return
        hotIsLoading = true

        resultModel.hotSuggest({
            runCatching {
                val list = mutableListOf<String>()
                for (hot in it.result.hots) {
                    list.add(hot.first)
                }
                updateState { copy(hotKeywords = list.toList()) }
            }
            isSearching = false
        }, { _, _ ->
            isSearching = false
        })
    }

    private fun getRelativeSuggest(keywords: String = state.searchContent) {
        if (keywords.isBlank()) updateState { copy(suggestKeywords = null) }
        if (suggestIsLoading) return
        suggestIsLoading = true
        resultModel.relativeSuggest(keywords, {
            runCatching {
                val list = mutableListOf<String>()
                for (match in it.result.allMatch) {
                    list.add(match.keyword)
                }
                updateState { copy(hotKeywords = list.toList()) }
                sendEffect { ResultEffect.KeywordSuccessEffect }
            }
            suggestIsLoading = false
        }, { _, _ ->
            updateState { copy(hotKeywords = null) }
            sendEffect { ResultEffect.KeywordsFailedEffect }
            suggestIsLoading = false
        })
    }

    // State-only
    private fun searchSongs(keywords: String = state.searchContent) {
        state.run {
            if (keywords.isBlank() || !searchHasMore || isSearching) return@run
            isSearching = true

            resultModel.searchSongs(keywords,
                SEARCH_LIMIT,
                searchCurrentPage * SEARCH_LIMIT,
                { data ->
                    runCatching {
                        data.result.songs.let { songs ->
                            val list = idList?.toMutableList() ?: mutableListOf()
                            for (song in songs)
                                list.add(song.id)
                            updateState { copy(idList = list) }

                            getSongs(data, list)

                            if (keywords != searchContent) {
                                resetResult()
                                searchSongs()
                            }
                        }
                    }.onFailure {
                        resetResult()
                    }
                    isSearching = false
                },
                { code, _ ->
                    if (code == null)
                        toast("网络错误")
                    else
                        toast("操作过于频繁, 请稍候重试")
                    isSearching = false
                })
        }
    }

    private fun getSongs(data: SearchResponse, list: List<String>) {
        getSongsWithIds(list) { songList ->
            if (!songList.isNullOrEmpty()) {
                updateState {
                    copy(
                        searchHasMore = data.result.hasMore,
                        searchCurrentPage = searchCurrentPage + 1,
                        songList = songList
                    )
                }
            }
        }
    }
}