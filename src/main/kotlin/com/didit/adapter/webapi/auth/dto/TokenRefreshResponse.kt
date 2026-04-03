package com.didit.adapter.webapi.auth.dto

data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
)
