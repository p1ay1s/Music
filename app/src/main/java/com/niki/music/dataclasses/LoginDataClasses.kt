package com.niki.music.dataclasses

data class AnonymousLoginResponse(
    var code: Int,
    var userId: String = "",
    var cookie: String = ""
)

data class LogoutResponse(
    var code: Int
)

/**
 * 刷新 - cookie的打开方式不对，所以这个数据还是不用的好
 */
data class RefreshCookieResponse(
    var code: Int,
    var cookie: String = ""
)

data class SendCaptchaResponse(
    var code: Int,
    var data: Boolean = false
)

/**
 * 通过检测是否注册的接口获取头像
 */
data class UserExistApiResponse(
    var exist: Int = -1,
    var code: Int,
    var avatarUrl: String = ""
)

/**
 * 登录状态
 */
data class LoginStateApiResponse(
    var data: LoginResponse = LoginResponse(-1, Account(), Profile())
)

data class LoginResponse(
    var code: Int,
    var account: Account,
    var profile: Profile,
    var cookie: String = ""
)

data class Account(
    var anonimousUser: Boolean = true,
    var id: String = ""
)

/**
 * 包含用户的基本资料
 */
data class Profile(
    var userId: String = "",
    var avatarUrl: String = "",
    var backgroundUrl: String = "",
    var nickname: String = ""
)
