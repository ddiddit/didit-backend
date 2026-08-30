package com.didit.adapter.webapi.auth.dto

import com.didit.application.auth.dto.SocialLoginResult
import com.didit.application.auth.dto.SocialLoginStatus
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialCredentialType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SocialLoginRequest(
    val provider: Provider,
    val credentialType: SocialCredentialType,
    @field:NotBlank val credential: String,
    val redirectUri: String? = null,
)

data class EmailVerificationStartRequest(
    @field:NotBlank val loginSessionToken: String,
    @field:NotBlank @field:Email val email: String,
)

data class EmailVerificationVerifyRequest(
    @field:NotBlank val loginSessionToken: String,
    @field:NotBlank val code: String,
)

data class SocialLoginResponse(
    val status: SocialLoginStatus,
    val accessToken: String?,
    val refreshToken: String?,
    val isNewUser: Boolean?,
    val isOnboardingCompleted: Boolean?,
    val loginSessionToken: String?,
    val emailHint: String?,
) {
    companion object {
        fun from(result: SocialLoginResult) =
            SocialLoginResponse(
                status = result.status,
                accessToken = result.token?.accessToken,
                refreshToken = result.token?.refreshToken,
                isNewUser = result.token?.isNewUser,
                isOnboardingCompleted = result.token?.isOnboardingCompleted,
                loginSessionToken = result.loginSessionToken,
                emailHint = result.emailHint,
            )
    }
}

data class EmailVerificationStartResponse(
    val expiresInSeconds: Long,
)
