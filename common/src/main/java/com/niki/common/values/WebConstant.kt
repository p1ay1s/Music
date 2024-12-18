package com.niki.common.values

/**
 * 存放各个功能的 API 接口
 */
object WebConstant {
    // 用户相关接口
    const val USER_CHECK_EXISTENCE = "cellphone/existence/check"
    const val SEND_CAPTCHA = "captcha/sent"
    const val USER_LOGIN_WITH_CAPTCHA = "login/cellphone"
    const val USER_LOGIN_REFRESH = "login/refresh"

    // 最流行歌单
    const val PLAYLIST_TOP = "top/playlist"

    // 搜索
    const val SEARCH = "search"
    const val SEARCH_HOT = "search/hot"
    const val SEARCH_RELATIVE = "search/suggest"

    // 歌曲详情
    const val SEARCH_SONGS_DETAIL = "song/detail"

    const val SONGS_FROM_ALBUM = "album"

    // 音乐相关接口
    const val SONGS_FROM_PLAYLIST = "playlist/track/all"
    const val SONG_INFO = "song/url/v1"
    const val SONG_AVAILABILITY = "check/music"

    // 用户喜好
    const val USER_LIKELIST = "likelist"
    const val USER_ALBUM = "album/sublist"
}