package com.niki.common.services

import com.niki.common.repository.dataclasses.AvailablityResponse
import com.niki.common.repository.dataclasses.GetSongInfoApiResponse
import com.niki.common.repository.dataclasses.GetSongsFromPlaylistApiResponse
import com.niki.common.values.WebConstant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PlayerService {
    @GET(WebConstant.SONG_AVAILABILITY)
    fun checkSong(
        @Query("id") id: String
    ): Call<AvailablityResponse>

    @GET(WebConstant.SONG_INFO)
    fun getSongInfo(
        @Query("id") id: String,
        @Query("level") level: String,
        @Query("cookie") cookie: String?
    ): Call<GetSongInfoApiResponse>

    @GET(WebConstant.SONGS_FROM_PLAYLIST)
    fun getSongsFromPlaylist(
        @Query("id") id: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<GetSongsFromPlaylistApiResponse>

    @GET(WebConstant.SEARCH_SONGS_DETAIL)
    fun getSongsWithIds(
        @Query("ids") ids: String
    ): Call<GetSongsFromPlaylistApiResponse>
}