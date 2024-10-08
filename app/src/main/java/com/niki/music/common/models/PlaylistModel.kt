package com.niki.music.common.models

import com.niki.music.dataclasses.CatePlaylistResponse
import com.niki.music.dataclasses.LikePlaylistResponse
import com.niki.music.dataclasses.TopPlaylistResponse
import com.niki.music.services.PlaylistService
import com.p1ay1s.dev.util.ServiceBuilder
import com.p1ay1s.dev.util.ServiceBuilder.makeRequest

class PlaylistModel {
    val playlistService by lazy {
        ServiceBuilder.create<PlaylistService>()
    }

    /**
     * 获取分类歌单信息
     */
    inline fun getCatePlaylists(
        crossinline onSuccess: (CatePlaylistResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(playlistService.getCatePlaylists(), onSuccess, onError)

    /**
     * 获取最流行的歌单信息
     */
    inline fun getTopPlaylists(
        limit: Int,
        order: String,
        offset: Int,
        crossinline onSuccess: (TopPlaylistResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(playlistService.getTopPlaylists(limit, order, offset), onSuccess, onError)

    /**
     * 获取(登录后)的用户收藏歌单信息
     */
    inline fun getLikePlaylist(
        uid: String,
        cookie: String,
        crossinline onSuccess: (LikePlaylistResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(playlistService.getLikePlaylist(uid, cookie), onSuccess, onError)
}