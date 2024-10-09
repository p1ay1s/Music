package com.niki.common.services

import com.niki.common.repository.dataclasses.AnonymousLoginResponse
import com.niki.common.repository.dataclasses.LoginResponse
import com.niki.common.repository.dataclasses.LoginStateApiResponse
import com.niki.common.repository.dataclasses.LogoutResponse
import com.niki.common.repository.dataclasses.RefreshCookieResponse
import com.niki.common.repository.dataclasses.SendCaptchaResponse
import com.niki.common.repository.dataclasses.UserExistApiResponse
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