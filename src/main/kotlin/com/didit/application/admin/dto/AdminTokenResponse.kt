package com.didit.application.admin.dto

data class AdminTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
