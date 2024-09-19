package com.niki.music.common

import androidx.lifecycle.MutableLiveData
import com.niki.music.model.Playlist
import com.niki.music.model.Song
import com.niki.music.model.Tag

object MusicRepository {
    // 关于 top
    var mTopPlaylists = listOf<Playlist>()

    // 关于 hot
    var mHotPlaylists = listOf<Tag>()

    // 关于 search
    var mSearchPlaylist = listOf<Song>()

    // 关于 like
    var mLikePlaylist = listOf<Song>()
}