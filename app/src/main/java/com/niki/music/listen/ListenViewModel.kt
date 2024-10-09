package com.niki.music.listen

import androidx.lifecycle.viewModelScope
import com.niki.common.repository.MusicRepository
import com.niki.music.common.viewModels.BaseViewModel
import com.p1ay1s.dev.base.TAG
import com.p1ay1s.dev.log.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListenViewModel : BaseViewModel<ListenIntent, ListenState, ListenEffect>() {

    companion object {
        const val TOP_LIMIT = 10
    }

    override fun initUiState() = ListenState(true, 0)

    override fun handleIntent(intent: ListenIntent): Unit =
        intent.run {
            logE(TAG, "RECEIVED " + this::class.simpleName.toString())
            when (this) {
                is ListenIntent.GetTopPlaylists -> getTopPlaylists(resetPage)
            }
        }

    private fun getTopPlaylists(
        resetPage: Boolean = false,
    ) = viewModelScope.launch(Dispatchers.IO) {
        uiStateFlow.value.run {
            if (!topHasMore) return@run

            if (resetPage)
                updateState { copy(topCurrentPage = 0) }

            playlistModel.getTopPlaylists(
                TOP_LIMIT,
                "hot",
                topCurrentPage * TOP_LIMIT,
                {
                    MusicRepository.topPlaylists =
                        MusicRepository.topPlaylists.plus(it.playlists)
                    updateState {
                        copy(
                            topHasMore = it.more,
                            topCurrentPage = topCurrentPage + 1,
                        )
                    }
                    sendEffect { ListenEffect.GetTopPlaylistsEffect(true) }
                },
                { _, _ -> sendEffect { ListenEffect.GetTopPlaylistsEffect() } })
        }
    }
}