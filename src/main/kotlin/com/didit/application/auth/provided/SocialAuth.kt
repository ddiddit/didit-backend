package com.didit.application.auth.provided

import com.didit.application.auth.dto.EmailVerificationStartResult
import com.didit.application.auth.dto.SocialLoginResult
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialCredentialType

interface SocialAuth {
    fun login(
        provider: Provider,
        credentialType: SocialCredentialType,
        credential: String,
        redirectUri: String?,
    ): SocialLoginResult

    fun startEmailVerification(
        loginSessionToken: String,
        email: String,
    ): EmailVerificationStartResult

    fun verifyEmail(
        loginSessionToken: String,
        code: String,
    ): SocialLoginResult
}
