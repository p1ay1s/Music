package com.niki.music.my.login

import com.niki.common.repository.dataclasses.login.loginApi.LoginResponse
import com.niki.common.repository.dataclasses.login.refreshCookieApi.RefreshCookieResponse
import com.niki.common.repository.dataclasses.login.sendCaptchaApi.SendCaptchaResponse
import com.niki.common.repository.dataclasses.login.userExistApi.UserExistResponse
import com.niki.common.services.LoginService
import com.p1ay1s.util.ServiceBuilder
import com.p1ay1s.util.ServiceBuilder.requestEnqueue

class LoginModel {
    val loginService by lazy {
        ServiceBuilder.create<LoginService>()
    }

    inline fun getAvatarUrl(
        phone: String,
        crossinline onSuccess: (UserExistResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.getAvatarUrl(phone), onSuccess, onError)

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

    inline fun passwordLogin(
        phone: String,
        password: String,
        crossinline onSuccess: (LoginResponse) -> Unit,
        crossinline onError: (Int?, String) -> Unit
    ) = requestEnqueue(loginService.passwordLogin(phone, password), onSuccess, onError)

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