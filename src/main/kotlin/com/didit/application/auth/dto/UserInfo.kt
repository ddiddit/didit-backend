package com.didit.application.auth.dto

import com.didit.domain.auth.Provider

data class UserInfo(
    val providerId: String,
    val provider: Provider,
    val nickname: String,
)
