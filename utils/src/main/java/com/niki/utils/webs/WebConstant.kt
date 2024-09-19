package com.niki.utils.webs

/**
 * 存放各个功能的 API 接口
 */
object WebConstant {
    // 用户相关接口
    const val ANONYMOUS_LOGIN = "register/anonimous"
    const val USER_CHECK_EXISTENCE = "cellphone/existence/check"
    const val SEND_CAPTCHA = "captcha/sent"
    const val USER_LOGIN_WITH_CAPTCHA = "login/cellphone"
    const val USER_LOGIN_STATUS = "login/status"
    const val USER_LOGIN_REFRESH = "login/refresh"
    const val USER_LOGOUT = "logout"

    // 热门歌单
    const val PLAYLIST_HOT = "playlist/hot"

    // 歌单分类
    const val PLAYLIST_CATE = "playlist/catlist"

    // 最流行歌单
    const val PLAYLIST_TOP = "top/playlist"

    // 搜索
    const val SEARCH = "search"

    // 歌曲详情
    const val SEARCH_SONGS_DETAIL = "song/detail"

    // 音乐相关接口
    const val SONGS_FROM_PLAYLIST = "playlist/track/all"
    const val SONG_INFO = "song/url/v1"
    const val SONG_AVAILABILITY = "check/music"

    // 用户喜好
    const val USER_LIKELIST = "likelist"
}