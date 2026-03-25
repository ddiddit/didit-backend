package com.didit.application.auth.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean = false,
)
