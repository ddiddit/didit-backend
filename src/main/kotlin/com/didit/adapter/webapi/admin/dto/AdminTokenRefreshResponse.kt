package com.didit.adapter.webapi.admin.dto

data class AdminTokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
)
