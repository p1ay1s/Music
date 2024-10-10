package com.niki.music.common.viewModels

import androidx.lifecycle.viewModelScope
import com.niki.music.common.models.PlayerModel
import com.niki.music.common.models.PlaylistModel
import com.niki.common.repository.dataclasses.Song
import com.p1ay1s.dev.base.MVIViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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