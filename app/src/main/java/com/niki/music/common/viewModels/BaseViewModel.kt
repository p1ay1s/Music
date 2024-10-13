package com.niki.music.common.viewModels

import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.models.PlayerModel
import com.niki.music.models.PlaylistModel
import com.p1ay1s.base.MVIViewModel

abstract class BaseViewModel<Intent, State, Effect> : MVIViewModel<Intent, State, Effect>() {
    protected val playerModel by lazy { PlayerModel() }
    protected val playlistModel by lazy { PlaylistModel() }

    protected inline fun getSongsWithIds(
        ids: List<String>,
        crossinline callback: (songList: List<Song>?) -> Unit
    ) = playerModel.getSongsWithIds(ids.joinToString(","),
        { data -> callback(data.songs) },
        { _, _ -> callback(null) })
}