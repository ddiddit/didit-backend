package com.didit.application.admin.dto

data class AdminRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
)
