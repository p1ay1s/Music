package com.niki.music.search.result

import androidx.lifecycle.viewModelScope
import com.niki.common.repository.MusicRepository
import com.niki.common.repository.dataclasses.SearchApiResponse
import com.niki.music.common.viewModels.BaseViewModel
import com.p1ay1s.dev.base.TAG
import com.p1ay1s.dev.base.toast
import com.p1ay1s.dev.log.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ResultViewModel : BaseViewModel<ResultIntent, ResultState, ResultEffect>() {
    private val resultModel by lazy { ResultModel() }

    companion object {
        const val SEARCH_LIMIT = 20
    }

    private var job: Job? = null

    private var shouldClean = false

    override fun initUiState() = ResultState("", true, 0)

    override fun handleIntent(intent: ResultIntent) =
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
            when (this) {
                is ResultIntent.SearchSongs -> {
                    uiStateFlow.value.run {
                        searchSongs(searchContent)
                    }
                }

                is ResultIntent.KeywordsChanged -> {
                    MusicRepository.searchPlaylist = emptyList()
                    shouldClean = true
                    updateState { copy(searchContent = keywords) }
                }
            }
            Unit
        }

    private fun searchSongs(keywords: String?) = uiStateFlow.value.run {
        if (keywords.isNullOrBlank() || !searchHasMore) return@run

        viewModelScope.launch {
            job?.cancel()
            job?.join()
            job = launch(Dispatchers.IO) Job@{ // 加标签解决 scope 重名冲突问题
                delay(200) // 冷静期
                resultModel.searchSongs(keywords,
                    SEARCH_LIMIT,
                    searchCurrentPage * SEARCH_LIMIT,
                    { data ->
                        data.result?.songs?.let { songs ->
                            val list = mutableListOf<String>()
                            for (song in songs)
                                list.add(song.id!!)

                            getSongs(data, list)
                        } ?: sendEffect {
                            ResultEffect.SearchSongsState()
                        }
                    },
                    { _, msg ->
                        toast(msg)
                        sendEffect {
                            ResultEffect.SearchSongsState()
                        }
                    })
            }
        }
    }

    private fun getSongs(data: SearchApiResponse, list: List<String>) {
        getSongsWithIds(list) { songList ->
            if (!songList.isNullOrEmpty()) {
                if (shouldClean) {
                    MusicRepository.searchPlaylist = emptyList()
                    shouldClean = false
                }

                MusicRepository.searchPlaylist =
                    MusicRepository.searchPlaylist.plus(songList)

                updateState {
                    copy(
                        searchHasMore = data.result!!.hasMore,
                        searchCurrentPage = searchCurrentPage + 1,
                    )
                }
                sendEffect { ResultEffect.SearchSongsState(true) }
            } else
                sendEffect {
                    ResultEffect.SearchSongsState()
                }
        }
    }
}