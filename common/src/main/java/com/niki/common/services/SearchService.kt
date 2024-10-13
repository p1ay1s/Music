package com.niki.common.services

import com.niki.common.repository.dataclasses.search.searchApi.SearchResponse
import com.niki.common.values.WebConstant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchService {
    @GET(WebConstant.SEARCH)
    fun searchSongs(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<SearchResponse>

//    @GET(WebConstant.SEARCH_HOT)
//    fun hotSuggest():Call<>
}