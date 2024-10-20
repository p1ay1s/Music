package com.niki.music.viewModel

import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.model.PlayerModel
import com.niki.music.model.PlaylistModel
import com.p1ay1s.base.MVIViewModel

abstract class BaseViewModel<Intent, State, Effect> : MVIViewModel<Intent, State, Effect>() {
    protected val playerModel by lazy { PlayerModel() }
    protected val playlistModel by lazy { PlaylistModel() }

    val state
        get() = uiStateFlow.value

    protected inline fun getSongsWithIds(
        ids: List<String>,
        crossinline callback: (songList: List<Song>?) -> Unit
    ) = playerModel.getSongsWithIds(ids.joinToString(","),
        { data -> callback(data.songs) },
        { _, _ -> callback(null) })
}