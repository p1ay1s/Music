package com.niki.common.repository.dataclasses.song.availabilityApi

data class AvailabilityResponse(
    var code: Int,
    var success: Boolean = false,
    var message: String = ""
)