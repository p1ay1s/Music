package com.niki.music.services

import com.niki.music.dataclasses.AnonymousLoginResponse
import com.niki.music.dataclasses.LoginResponse
import com.niki.music.dataclasses.LoginStateApiResponse
import com.niki.music.dataclasses.LogoutResponse
import com.niki.music.dataclasses.RefreshCookieResponse
import com.niki.music.dataclasses.SendCaptchaResponse
import com.niki.music.dataclasses.UserExistApiResponse
import com.niki.utils.webs.WebConstant
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

    @GET(WebConstant.USER_LOGIN_STATUS)
    fun loginState(
        @Query("cookie") cookie: String? = null
    ): Call<LoginStateApiResponse>

    @GET(WebConstant.USER_LOGIN_REFRESH)
    fun loginRefresh(
        @Query("cookie") cookie: String? = null
    ): Call<RefreshCookieResponse>

    @GET(WebConstant.USER_LOGOUT)
    fun logout(
        @Query("cookie") cookie: String? = null
    ): Call<LogoutResponse>
}