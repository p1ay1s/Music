package com.niki.music.search

import com.niki.music.model.SearchApiResponse
import com.niki.music.services.SearchService
import com.niki.utils.webs.ServiceBuilder
import com.niki.utils.webs.ServiceBuilder.makeRequest

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