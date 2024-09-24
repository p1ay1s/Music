package com.niki.music.services

import com.niki.music.dataclasses.SearchApiResponse
import com.niki.utils.webs.WebConstant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchService {
    @GET(WebConstant.SEARCH)
    fun searchSongs(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<SearchApiResponse>
}