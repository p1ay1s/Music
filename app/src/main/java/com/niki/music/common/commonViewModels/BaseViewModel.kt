package com.niki.music.common.commonViewModels

import com.niki.music.common.models.PlayerModel
import com.niki.music.common.models.PlaylistModel
import com.niki.music.model.Song
import com.niki.utils.base.BaseViewModel

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