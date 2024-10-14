package com.niki.music.search.result

import com.niki.common.repository.dataclasses.search.hotRecommendApi.SearchHotResponse
import com.niki.common.repository.dataclasses.search.searchApi.SearchResponse
import com.niki.common.repository.dataclasses.search.searchRecommendApi.SearchSuggestResponse
import com.niki.common.services.SearchService
import com.p1ay1s.util.ServiceBuilder
import com.p1ay1s.util.ServiceBuilder.requestEnqueue

class ResultModel {
    val searchService by lazy {
        ServiceBuilder.create<SearchService>()
    }

    /**
     * 使用关键词搜索
     */
    inline fun searchSongs(
        keywords: String,
        limit: Int,
        offset: Int,
        crossinline onSuccess: (SearchResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(searchService.searchSongs(keywords, limit, offset), onSuccess, onError)

    inline fun hotSuggest(
        crossinline onSuccess: (SearchHotResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(searchService.hotSuggest(), onSuccess, onError)

    inline fun relativeSuggest(
        keywords: String,
        crossinline onSuccess: (SearchSuggestResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(
        searchService.relativeSuggest(
            keywords, "mobile"
        ), onSuccess, onError
    )
}