package com.didit.application.auth.dto

enum class SocialLoginStatus {
    AUTHENTICATED,
    EMAIL_VERIFICATION_REQUIRED,
    SUPPORT_REQUIRED,
}

data class SocialLoginResult(
    val status: SocialLoginStatus,
    val token: TokenResponse? = null,
    val loginSessionToken: String? = null,
    val emailHint: String? = null,
)

data class EmailVerificationStartResult(
    val expiresInSeconds: Long,
)
