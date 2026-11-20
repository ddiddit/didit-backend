package com.didit.application.auth.dto

data class AppleTokenExchange(
    val userInfo: UserInfo,
    val clientId: String,
    val refreshToken: String,
)
