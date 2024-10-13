package com.niki.common.repository.dataclasses.login.refreshCookieApi

data class RefreshCookieResponse(
    var code: Int,
    var cookie: String = ""
)