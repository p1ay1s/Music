package com.niki.music.my.login

import com.niki.music.model.AnonymousLoginResponse
import com.niki.music.model.LoginResponse
import com.niki.music.model.LogoutResponse
import com.niki.music.model.RefreshCookieResponse
import com.niki.music.model.SendCaptchaResponse
import com.niki.music.model.UserExistApiResponse
import com.niki.music.services.LoginService
import com.niki.base.util.ServiceBuilder
import com.niki.base.util.ServiceBuilder.makeRequest

class LoginModel {
    val loginService by lazy {
        ServiceBuilder.create<LoginService>()
    }

    inline fun logout(
        cookie: String,
        crossinline onSuccess: (LogoutResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(loginService.logout(cookie), onSuccess, onError)

    inline fun getAvatarUrl(
        phone: String,
        crossinline onSuccess: (UserExistApiResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(loginService.getAvatarUrl(phone), onSuccess, onError)

    /**
     * 游客登录
     * 主要是为了cookie
     */
    inline fun anonymousLogin(
        crossinline onSuccess: (AnonymousLoginResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(loginService.anonymousLogin(), onSuccess, onError)

    /**
     * 使用手机号码和验证码登录
     * 成功后返回用户的各类信息
     */
    inline fun captchaLogin(
        phone: String,
        captcha: String,
        crossinline onSuccess: (LoginResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(loginService.captchaLogin(phone, captcha), onSuccess, onError)

    /**
     * 发送验证码到对应的手机号码
     */
    inline fun sendCaptcha(
        phone: String,
        crossinline onSuccess: (SendCaptchaResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(loginService.sendCaptcha(phone), onSuccess, onError)

    /**
     * 更新 cookie
     */
    inline fun loginRefresh(
        cookie: String,
        crossinline onSuccess: (RefreshCookieResponse) -> Unit,
        crossinline onError: (Int, String) -> Unit
    ) = makeRequest(loginService.loginRefresh(cookie), onSuccess, onError)
}