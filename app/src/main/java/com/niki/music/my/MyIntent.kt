package com.niki.music.my

import com.niki.common.repository.dataclasses.song.Song

sealed class MyIntent {
    data class UpdatePhone(val phone: String?) : MyIntent()
    data class UpdateCaptcha(val captcha: String?) : MyIntent()
    data object GetAvatarUrl : MyIntent()
    data object CaptchaLogin : MyIntent()
    data object PasswordLogin : MyIntent()
    data object SendCaptcha : MyIntent()
    data object SwitchMethod : MyIntent()
    data object GetLikePlaylist : MyIntent()
    data object Logout : MyIntent()
}

sealed class MyEffect {
    data class GetAvatarUrlOkEffect(val url: String) : MyEffect()
    data object GetAvatarUrlBadEffect : MyEffect()
}

data class LoggedInDatas(
    val userId: String,
    val nickname: String,
    val cookie: String,
    val avatarUrl: String,
    val backgroundUrl: String
)

data class MyState(
    val phone: String?,
    val captcha: String?,
    val isLoggedIn: Boolean,
    val useCaptcha: Boolean,
    val loggedInDatas: LoggedInDatas?,
    val likeList: List<Song>?
)