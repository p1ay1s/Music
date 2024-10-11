package com.niki.music.search.result

import com.niki.common.repository.dataclasses.SearchApiResponse
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
        crossinline onSuccess: (SearchApiResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = requestEnqueue(searchService.searchSongs(keywords, limit, offset), onSuccess, onError)
}