package com.niki.music.common.models

import com.niki.music.dataclasses.AvailableResponse
import com.niki.music.dataclasses.GetSongInfoApiResponse
import com.niki.music.dataclasses.GetSongsFromPlaylistApiResponse
import com.niki.music.services.PlayerService
import com.p1ay1s.dev.util.ServiceBuilder
import com.p1ay1s.dev.util.ServiceBuilder.makeRequest

class PlayerModel {
    val playerService by lazy {
        ServiceBuilder.create<PlayerService>()
    }

    /**
     * 检查是否是一首有效的歌曲
     */
    inline fun checkSongAbility(
        id: String,
        crossinline onSuccess: (AvailableResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(playerService.checkSong(id), onSuccess, onError)

    /**
     * 通过 id 等信息获取播放的必要内容(如歌曲的 url 以及歌名等)
     */
    inline fun getSongInfo(
        id: String,
        level: String,
        cookie: String?,
        crossinline onSuccess: (GetSongInfoApiResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(playerService.getSongInfo(id, level, cookie), onSuccess, onError)

    /**
     * 从歌单分页获取若干首歌曲
     */
    inline fun getSongsFromPlaylist(
        id: String,
        limit: Int,
        offset: Int,
        crossinline onSuccess: (GetSongsFromPlaylistApiResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(playerService.getSongsFromPlaylist(id, limit, offset), onSuccess, onError)


    /**
     * 用搜索结果的 ids 进一步获取结果歌单
     */
    inline fun getSongsWithIds(
        ids: String,
        crossinline onSuccess: (GetSongsFromPlaylistApiResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(playerService.getSongsWithIds(ids), onSuccess, onError)
}