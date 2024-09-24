package com.niki.music.common.viewModels

import com.niki.music.common.models.PlayerModel
import com.niki.music.common.models.PlaylistModel
import com.niki.music.dataclasses.Song
import com.niki.base.BaseViewModel

abstract class BaseViewModel<Intent, State, Effect> : BaseViewModel<Intent, State, Effect>() {
    protected val playerModel by lazy { PlayerModel() }
    protected val playlistModel by lazy { PlaylistModel() }

    protected inline fun getSongsWithIds(
        ids: List<String>,
        crossinline callback: (songList: List<Song>?) -> Unit
    ) = playerModel.getSongsWithIds(ids.joinToString(","),
        { data -> callback(data.songs) },
        { _, _ -> callback(null) })
}