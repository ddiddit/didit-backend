package com.didit.adapter.webapi.admin.dto

data class AdminLoginResponse(
    val accessToken: String,
    val refreshToken: String,
)
