package com.niki.music.services

import com.niki.music.model.AnonymousLoginResponse
import com.niki.music.model.LoginResponse
import com.niki.music.model.LoginStateApiResponse
import com.niki.music.model.LogoutResponse
import com.niki.music.model.RefreshCookieResponse
import com.niki.music.model.SendCaptchaResponse
import com.niki.music.model.UserExistApiResponse
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