package com.didit.domain.admin

data class AdminRegisterRequest(
    val email: String,
    val encodedPassword: String,
    val position: AdminPosition,
)
