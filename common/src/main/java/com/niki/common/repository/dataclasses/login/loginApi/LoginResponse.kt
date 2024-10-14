package com.niki.common.repository.dataclasses.login.loginApi

data class LoginResponse(
    val account: Account,
    val bindings: List<Binding>,
    val clientId: String,
    val code: Int,
    val cookie: String,
    val effectTime: Int,
    val loginType: Int,
    val profile: Profile,
    val token: String,
    val message: String?
)