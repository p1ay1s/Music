package com.niki.music.model

import com.niki.common.repository.dataclasses.playlist.topPlaylistApi.TopPlaylistResponse
import com.niki.common.repository.dataclasses.song.likeAlbumApi.LikeAlbumResponse
import com.niki.common.repository.dataclasses.song.likeListApi.LikePlaylistResponse
import com.niki.common.services.PlaylistService
import com.p1ay1s.util.ServiceBuilder
import com.p1ay1s.util.ServiceBuilder.requestEnqueue

class PlaylistModel {
    val playlistService by lazy {
        ServiceBuilder.create<PlaylistService>()
    }

    /**
     * 获取最流行的歌单信息
     */
    inline fun getTopPlaylists(
        limit: Int,
        order: String,
        offset: Int,
        crossinline onSuccess: (TopPlaylistResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playlistService.getTopPlaylists(limit, order, offset), onSuccess, onError)

    /**
     * 获取(登录后)的用户收藏歌单信息
     */
    inline fun getLikePlaylist(
        uid: String,
        cookie: String,
        crossinline onSuccess: (LikePlaylistResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playlistService.getLikePlaylist(uid, cookie), onSuccess, onError)

    /**
     * 收藏的专辑
     *
     * 可以用 subTime 来排序
     */
    inline fun getLikeAlbums(
        cookie: String,
        limit: Int,
        offset: Int,
        crossinline onSuccess: (LikeAlbumResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playlistService.getLikeAlbums(cookie, limit, offset), onSuccess, onError)
}