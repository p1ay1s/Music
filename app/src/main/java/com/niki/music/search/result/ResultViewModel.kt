package com.niki.music.search.result

import androidx.lifecycle.viewModelScope
import com.niki.common.repository.dataclasses.SearchApiResponse
import com.niki.music.common.viewModels.BaseViewModel
import com.p1ay1s.base.extension.TAG
import com.p1ay1s.base.extension.toast
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ResultViewModel : BaseViewModel<ResultIntent, ResultState, ResultEffect>() {
    private val resultModel by lazy { ResultModel() }

    companion object {
        const val SEARCH_LIMIT = 20
    }

    private var job: Job? = null

    override fun initUiState() = ResultState("", true, 0, null)

    override fun handleIntent(intent: ResultIntent) =
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
            when (this) {
                is ResultIntent.SearchSongs -> searchSongs(uiStateFlow.value.searchContent)

                is ResultIntent.KeywordsChanged ->
                    if (uiStateFlow.value.searchContent != keywords)
                        updateState {
                            copy(
                                searchContent = keywords,
                                songList = null
                            )
                        }
            }
        }

    // State-only
    private fun searchSongs(keywords: String?) = uiStateFlow.value.run {
        if (keywords.isNullOrBlank() || !searchHasMore) return@run

        viewModelScope.launch {
            job?.cancel()
            job?.join()
            job = launch(Dispatchers.IO) Job@{ // 加标签解决 scope 重名冲突问题
//                delay(200) // 冷静期
                resultModel.searchSongs(keywords,
                    SEARCH_LIMIT,
                    searchCurrentPage * SEARCH_LIMIT,
                    { data ->
                        data.result?.songs?.let { songs ->
                            val list = mutableListOf<String>()
                            for (song in songs)
                                list.add(song.id!!)

                            getSongs(data, list)
                        }
                    },
                    { code, _ ->
                        if (code == null)
                            toast("网络错误")
                        else {
                            if (code == 405) "操作过于频繁, 请稍后再试".toast()
                        }
                    })
            }
        }
    }

    private fun getSongs(data: SearchApiResponse, list: List<String>) {
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