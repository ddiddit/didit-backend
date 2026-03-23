package com.didit.application.auth.dto

import com.didit.domain.auth.enums.SocialProvider

data class SocialLoginRequest(
    val provider: SocialProvider,
    val idToken: String,
)
