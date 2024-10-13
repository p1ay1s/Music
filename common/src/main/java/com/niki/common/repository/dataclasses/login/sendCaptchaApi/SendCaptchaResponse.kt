package com.niki.common.repository.dataclasses.login.sendCaptchaApi

data class SendCaptchaResponse(
    var code: Int,
    var data: Boolean = false
)