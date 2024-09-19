package com.niki.music.listen

sealed class ListenIntent {
    class GetTopPlaylists(val resetPage: Boolean = false) :
        ListenIntent()

    data object GetHotPlaylists : ListenIntent()
}

sealed class ListenEffect {
    data class GetTopPlaylistsEffect(val isSuccess: Boolean = false) : ListenEffect()
    data class GetHotPlaylistsEffect(val isSuccess: Boolean = false) : ListenEffect()
}

data class ListenState(
    val topHasMore: Boolean,
    val topCurrentPage: Int,
)