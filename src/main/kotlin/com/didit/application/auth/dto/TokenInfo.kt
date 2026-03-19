package com.didit.application.auth.dto

data class TokenInfo(
    val accessToken: String,
    val refreshToken: String,
)
