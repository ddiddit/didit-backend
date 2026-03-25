package com.didit.domain.auth

data class UserRegisterRequest(
    val provider: Provider,
    val providerId: String,
    val email: String?,
)
