package com.niki.common.repository.dataclasses.login.loginApi

data class Binding(
    val bindingTime: Long,
    val expired: Boolean,
    val expiresIn: Int,
    val id: Long,
    val refreshTime: Int,
    val tokenJsonStr: String,
    val type: Int,
    val url: String,
    val userId: Int
)