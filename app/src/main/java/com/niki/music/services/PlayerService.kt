package com.niki.music.services

import com.niki.music.dataclasses.AvailableResponse
import com.niki.music.dataclasses.GetSongInfoApiResponse
import com.niki.music.dataclasses.GetSongsFromPlaylistApiResponse
import com.niki.utils.webs.WebConstant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PlayerService {
    @GET(WebConstant.SONG_AVAILABILITY)
    fun checkSong(
        @Query("id") id: String
    ): Call<AvailableResponse>

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