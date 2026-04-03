package com.didit.application.auth.dto

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
)
