package com.niki.music.listen

import com.niki.common.repository.dataclasses.Playlist

sealed class ListenIntent {
    class GetTopPlaylists(val resetPage: Boolean = false) :
        ListenIntent()
}

sealed class ListenEffect {
}

data class ListenState(
    val hasMore: Boolean,
    val isLoading:Boolean,
    val currentPage: Int,
    var playlists: List<Playlist>?
)