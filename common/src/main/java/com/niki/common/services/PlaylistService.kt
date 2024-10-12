package com.niki.common.services

import com.niki.common.repository.dataclasses.CatePlaylistResponse
import com.niki.common.repository.dataclasses.LikePlaylistResponse
import com.niki.common.repository.dataclasses.TopPlaylistResponse
import com.niki.common.values.WebConstant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaylistService {
    @GET(WebConstant.PLAYLIST_CATE)
    fun getCatePlaylists(): Call<CatePlaylistResponse>

    @GET(WebConstant.PLAYLIST_TOP)
    fun getTopPlaylists(
        @Query("limit") limit: Int,
        @Query("order") order: String,
        @Query("offset") offset: Int
    ): Call<TopPlaylistResponse>

    @GET(WebConstant.USER_LIKELIST)
    fun getLikePlaylist(
        @Query("uid") uid: String,
        @Query("cookie") cookie: String
    ): Call<LikePlaylistResponse>
}