package com.niki.music.listen

import com.niki.music.common.MusicRepository
import com.niki.music.common.viewModels.BaseViewModel
import com.niki.utils.TAG
import com.niki.base.log.logE

class ListenViewModel : BaseViewModel<ListenIntent, ListenState, ListenEffect>() {
    companion object {
        const val TOP_LIMIT = 10
    }

    override fun initUiState() = ListenState(true, 0)

    override fun handleIntent(intent: ListenIntent) =
        intent.run {
            logE(TAG, "RECEIVED" + this::class.simpleName.toString())
            when (this) {
                is ListenIntent.GetTopPlaylists -> getTopPlaylists(resetPage)
            }
        }

    private fun getTopPlaylists(
        resetPage: Boolean = false,
    ) = uiStateFlow.value.run {
        if (!topHasMore) return

        if (resetPage)
            updateState { copy(topCurrentPage = 0) }

        playlistModel.getTopPlaylists(TOP_LIMIT,
            "hot",
            topCurrentPage * TOP_LIMIT,
            {
                MusicRepository.mTopPlaylists = MusicRepository.mTopPlaylists.plus(it.playlists)
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