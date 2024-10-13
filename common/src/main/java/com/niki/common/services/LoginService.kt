package com.niki.common.services

import com.niki.common.repository.dataclasses.login.loginApi.AnonymousLoginResponse
import com.niki.common.repository.dataclasses.login.loginApi.LoginResponse
import com.niki.common.repository.dataclasses.login.loginApi.LogoutResponse
import com.niki.common.repository.dataclasses.login.loginApi.RefreshCookieResponse
import com.niki.common.repository.dataclasses.login.loginApi.SendCaptchaResponse
import com.niki.common.repository.dataclasses.login.loginApi.UserExistApiResponse
import com.niki.common.values.WebConstant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface LoginService {
    @GET(WebConstant.ANONYMOUS_LOGIN)
    fun anonymousLogin(): Call<AnonymousLoginResponse>

    @GET(WebConstant.SEND_CAPTCHA)
    fun sendCaptcha(
        @Query("phone") phone: String,
    ): Call<SendCaptchaResponse>

    @GET(WebConstant.USER_LOGIN_WITH_CAPTCHA)
    fun captchaLogin(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String
    ): Call<LoginResponse>

    @GET(WebConstant.USER_CHECK_EXISTENCE)
    fun getAvatarUrl(
        @Query("phone") phone: String,
    ): Call<UserExistApiResponse>

    @GET(WebConstant.USER_LOGIN_REFRESH)
    fun loginRefresh(
        @Query("cookie") cookie: String? = null
    ): Call<RefreshCookieResponse>

    @GET(WebConstant.USER_LOGOUT)
    fun logout(
        @Query("cookie") cookie: String? = null
    ): Call<LogoutResponse>
}