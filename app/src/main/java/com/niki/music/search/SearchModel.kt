package com.niki.music.search

import com.niki.music.dataclasses.SearchApiResponse
import com.niki.music.services.SearchService
import com.p1ay1s.dev.util.ServiceBuilder
import com.p1ay1s.dev.util.ServiceBuilder.makeRequest

class SearchModel {
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
    ) = makeRequest(searchService.searchSongs(keywords, limit, offset), onSuccess, onError)
}