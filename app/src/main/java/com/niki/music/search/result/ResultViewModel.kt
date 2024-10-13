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

    private var isLoading = false

    override fun initUiState() = ResultState("", true, 0, null, null)

    override fun handleIntent(intent: ResultIntent) =
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
            when (this) {
                is ResultIntent.SearchSongs -> searchSongs()

                is ResultIntent.KeywordsChanged ->
                    if (uiStateFlow.value.searchContent != keywords) {
                        updateState { copy(searchContent = keywords) }
                        resetResult()
                    }
            }
        }

    private fun resetResult() {
        updateState {
            copy(
                searchCurrentPage = 0,
                searchHasMore = true,
                songList = null,
                idList = null
            )
        }
    }

    // State-only
    private fun searchSongs(keywords: String = uiStateFlow.value.searchContent) {
        uiStateFlow.value.run {
            if (keywords.isBlank() || !searchHasMore || isLoading) return@run
            isLoading = true

            resultModel.searchSongs(keywords,
                SEARCH_LIMIT,
                searchCurrentPage * SEARCH_LIMIT,
                { data ->
                    isLoading = false
                    data.result.songs.let { songs ->
                        val list = uiStateFlow.value.idList?.toMutableList() ?: mutableListOf()
                        for (song in songs)
                            list.add(song.id!!)
                        updateState { copy(idList = list) }

                        getSongs(data, list)

                        if (keywords != uiStateFlow.value.searchContent) {
                            resetResult()
                            searchSongs()
                        }
                    } ?: toast("操作过于频繁, 请稍候重试")
                },
                { code, _ ->
                    if (code == null)
                        toast("网络错误")
                    else
                        toast("操作过于频繁, 请稍候重试")
                    isLoading = false
                })
        }
    }

    private fun getSongs(data: SearchResponse, list: List<String>) {
        getSongsWithIds(list) { songList ->
            if (!songList.isNullOrEmpty()) {
                updateState {
                    copy(
                        searchHasMore = data.result!!.hasMore,
                        searchCurrentPage = searchCurrentPage + 1,
                        songList = songList
                    )
                }
            }
        }
    }
}