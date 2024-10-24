package com.niki.common.services

import com.niki.common.repository.dataclasses.login.loginApi.LoginResponse
import com.niki.common.repository.dataclasses.login.refreshCookieApi.RefreshCookieResponse
import com.niki.common.repository.dataclasses.login.sendCaptchaApi.SendCaptchaResponse
import com.niki.common.repository.dataclasses.login.userExistApi.UserExistResponse
import com.niki.common.values.WebConstant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface UserService {
    @GET(WebConstant.SEND_CAPTCHA)
    fun sendCaptcha(
        @Query("phone") phone: String,
    ): Call<SendCaptchaResponse>

    @GET(WebConstant.USER_LOGIN_WITH_CAPTCHA)
    fun captchaLogin(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String
    ): Call<LoginResponse>

    @GET(WebConstant.USER_LOGIN_WITH_CAPTCHA)
    fun passwordLogin(
        @Query("phone") phone: String,
        @Query("password") password: String
    ): Call<LoginResponse>

    @GET(WebConstant.USER_CHECK_EXISTENCE)
    fun getAvatarUrl(
        @Query("phone") phone: String,
    ): Call<UserExistResponse>

    @GET(WebConstant.USER_LOGIN_REFRESH)
    fun loginRefresh(
        @Query("cookie") cookie: String? = null
    ): Call<RefreshCookieResponse>
}