package com.niki.music.my

sealed class MyIntent {
    data class UpdatePhone(val phone: String?) : MyIntent()
    data class UpdateCaptcha(val captcha: String?) : MyIntent()
    data object GetAvatarUrl : MyIntent()
    data object CaptchaLogin : MyIntent()
    data object SendCaptcha : MyIntent()
    data object GetLikePlaylist : MyIntent()
    data object Logout : MyIntent()
}

sealed class MyEffect {
    data class GetAvatarUrlOkEffect(val url: String) : MyEffect()
    data object GetAvatarUrlBadEffect : MyEffect()
    data class GetLikePlaylistEffect(val isSuccess: Boolean = false) : MyEffect()
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
    val loggedInDatas: LoggedInDatas?,
)