package com.niki.music.my.login

import com.niki.common.repository.dataclasses.AnonymousLoginResponse
import com.niki.common.repository.dataclasses.LoginResponse
import com.niki.common.repository.dataclasses.LogoutResponse
import com.niki.common.repository.dataclasses.RefreshCookieResponse
import com.niki.common.repository.dataclasses.SendCaptchaResponse
import com.niki.common.repository.dataclasses.UserExistApiResponse
import com.niki.common.services.LoginService
import com.p1ay1s.util.ServiceBuilder
import com.p1ay1s.util.ServiceBuilder.requestEnqueue

class LoginModel {
    val loginService by lazy {
        ServiceBuilder.create<LoginService>()
    }

    inline fun logout(
        cookie: String,
        crossinline onSuccess: (LogoutResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.logout(cookie), onSuccess, onError)

    inline fun getAvatarUrl(
        phone: String,
        crossinline onSuccess: (UserExistApiResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.getAvatarUrl(phone), onSuccess, onError)

    /**
     * 游客登录
     * 主要是为了cookie
     */
    inline fun anonymousLogin(
        crossinline onSuccess: (AnonymousLoginResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.anonymousLogin(), onSuccess, onError)

    /**
     * 使用手机号码和验证码登录
     * 成功后返回用户的各类信息
     */
    inline fun captchaLogin(
        phone: String,
        captcha: String,
        crossinline onSuccess: (LoginResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.captchaLogin(phone, captcha), onSuccess, onError)

    /**
     * 发送验证码到对应的手机号码
     */
    inline fun sendCaptcha(
        phone: String,
        crossinline onSuccess: (SendCaptchaResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.sendCaptcha(phone), onSuccess, onError)

    /**
     * 更新 cookie
     */
    inline fun loginRefresh(
        cookie: String,
        crossinline onSuccess: (RefreshCookieResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.loginRefresh(cookie), onSuccess, onError)
}