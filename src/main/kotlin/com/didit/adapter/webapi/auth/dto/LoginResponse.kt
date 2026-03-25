package com.didit.adapter.webapi.auth.dto

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
    val isOnboardingCompleted: Boolean,
)
