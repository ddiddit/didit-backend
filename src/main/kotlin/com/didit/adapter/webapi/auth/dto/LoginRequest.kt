package com.didit.adapter.webapi.auth.dto

import com.didit.domain.auth.Provider

data class LoginRequest(
    val provider: Provider,
    val oauthToken: String,
)
