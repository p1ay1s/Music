package com.niki.common.repository.dataclasses.login.userExistApi

data class UserExistResponse(
    var exist: Int = -1,
    var code: Int,
    var avatarUrl: String = ""
)