package com.niki.music.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niki.common.repository.dataclasses.song.Song
import com.niki.music.model.PlayerModel
import com.niki.music.model.PlaylistModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch


abstract class NewBaseViewModel<Intent, State, Effect> : ViewModel() {
    protected val playerModel by lazy { PlayerModel() }
    protected val playlistModel by lazy { PlaylistModel() }

    private val channel = Channel<Intent>(Channel.UNLIMITED)

    protected val _state = MutableLiveData(initState())
    val state: LiveData<State>
        get() = _state

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> by lazy { _effect.asSharedFlow() }

    init {
        viewModelScope.launch {
            channel.consumeAsFlow().collect { handleIntent(it) }
        }
    }

    fun sendIntent(intent: Intent) =
        viewModelScope.launch {
            channel.send(intent)
        }

    protected fun sendEffect(builder: suspend () -> Effect?) = viewModelScope.launch {
        builder()?.let {
            _effect.emit(it)
        }
    }

    protected suspend fun sendEffect(effect: Effect) = _effect.emit(effect)

    protected abstract fun initState(): State

    protected abstract fun handleIntent(intent: Intent)

    protected inline fun getSongsWithIds(
        ids: List<String>,
        crossinline callback: (songList: List<Song>?) -> Unit
    ) = playerModel.getSongsWithIds(ids.joinToString(","),
        { data -> callback(data.songs) },
        { _, _ -> callback(null) })
}