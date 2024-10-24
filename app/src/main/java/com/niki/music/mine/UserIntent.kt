package com.niki.music.mine

import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.repository.dataclasses.song.likeAlbumApi.LikeAlbumData

sealed class UserIntent {
    data class UpdatePhone(val phone: String?) : UserIntent()
    data class UpdateCaptcha(val captcha: String?) : UserIntent()
    data object GetAvatarUrl : UserIntent()
    data object CaptchaLogin : UserIntent()
    data object PasswordLogin : UserIntent()
    data object SendCaptcha : UserIntent()
    data object SwitchMethod : UserIntent()
    data object GetLikePlaylist : UserIntent()
    data object GetLikeAlbums : UserIntent()
    data object Logout : UserIntent()
}

sealed class UserEffect {
    data class GetAvatarUrlOkEffect(val url: String) : UserEffect()
    data object GetAvatarUrlBadEffect : UserEffect()
}

data class LoggedInDatas(
    val userId: String,
    val nickname: String,
    val cookie: String,
    val avatarUrl: String,
    val backgroundUrl: String
)

data class UserState(
    val phone: String?,
    val captcha: String?,
    val isLoggedIn: Boolean,
    val useCaptcha: Boolean,
    val loggedInDatas: LoggedInDatas?,
    val likeList: List<Song>?,
    val likeAlbums: List<LikeAlbumData>?
)