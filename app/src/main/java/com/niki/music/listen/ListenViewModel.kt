package com.niki.music.listen

import androidx.lifecycle.viewModelScope
import com.niki.music.common.viewModels.BaseViewModel
import com.p1ay1s.base.extension.TAG
import com.p1ay1s.base.log.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListenViewModel : BaseViewModel<ListenIntent, ListenState, ListenEffect>() {

    companion object {
        const val TOP_LIMIT = 10
    }

    var position = 0

    override fun initUiState() = ListenState(hasMore = true, isLoading = false, 0, null)

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
            if (!hasMore || isLoading) return@run
            updateState { copy(isLoading = true) }

            if (resetPage)
                updateState { copy(currentPage = 0) }

            playlistModel.getTopPlaylists(
                TOP_LIMIT,
                "hot",
                currentPage * TOP_LIMIT,
                {
                    val currentList = uiStateFlow.value.playlists ?: emptyList()

                    updateState {
                        copy(
                            hasMore = it.more,
                            isLoading = false,
                            currentPage = currentPage + 1,
                            playlists = currentList + it.playlists
                        )
                    }
                },
                { code, msg ->
                    updateState { copy(isLoading = false) }
                    if (code != null) logE("###", msg)
                })
        }
    }
}