package com.niki.music.common

import com.niki.music.dataclasses.Playlist
import com.niki.music.dataclasses.Song
import com.niki.music.dataclasses.Tag

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