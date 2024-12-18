package com.niki.music.model

import com.niki.common.repository.dataclasses.album.AlbumResponse
import com.niki.common.repository.dataclasses.song.availabilityApi.AvailabilityResponse
import com.niki.common.repository.dataclasses.song.getSongInfoApi.GetSongInfoApiResponse
import com.niki.common.repository.dataclasses.song.getSongsFromPlaylistApi.GetSongsFromPlaylistApiResponse
import com.niki.common.services.PlayerService
import com.p1ay1s.util.ServiceBuilder
import com.p1ay1s.util.ServiceBuilder.requestEnqueue
import com.p1ay1s.util.ServiceBuilder.requestExecute
import com.p1ay1s.util.ServiceBuilder.requestSuspend

class PlayerModel {
    val playerService by lazy {
        ServiceBuilder.create<PlayerService>()
    }

    /**
     * 检查是否是一首有效的歌曲
     */
    inline fun checkSongAbility(
        id: String,
        crossinline onSuccess: (AvailabilityResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playerService.checkSong(id), onSuccess, onError)

    /**
     * 获取专辑中所有歌曲, 注意为了复用代码此处不能直接使用结果而应该用所有的 id 重新获取 song
     */
    inline fun getAlbumSongs(
        id: String,
        crossinline onSuccess: (AlbumResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playerService.getAlbumSongs(id), onSuccess, onError)

    /**
     * 通过 id 等信息获取播放的必要内容(如歌曲的 url 以及歌名等)
     */
    inline fun getSongInfo(
        id: String,
        level: String,
        cookie: String?,
        crossinline onSuccess: (GetSongInfoApiResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playerService.getSongInfo(id, level, cookie), onSuccess, onError)

    suspend inline fun getSongInfoSuspend(
        id: String,
        level: String,
        cookie: String?,
        crossinline onSuccess: (GetSongInfoApiResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestSuspend(playerService.getSongInfo(id, level, cookie), onSuccess, onError)

    inline fun getSongInfoExecute(
        id: String,
        level: String,
        cookie: String?,
        crossinline onSuccess: (GetSongInfoApiResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestExecute(playerService.getSongInfo(id, level, cookie), onSuccess, onError)

    /**
     * 从歌单分页获取若干首歌曲
     */
    inline fun getSongsFromPlaylist(
        id: String,
        limit: Int,
        offset: Int,
        crossinline onSuccess: (GetSongsFromPlaylistApiResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playerService.getSongsFromPlaylist(id, limit, offset), onSuccess, onError)


    /**
     * 用搜索结果的 ids 进一步获取结果歌单
     */
    inline fun getSongsWithIds(
        ids: String,
        crossinline onSuccess: (GetSongsFromPlaylistApiResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(playerService.getSongsWithIds(ids), onSuccess, onError)
}