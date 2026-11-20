package com.didit.application.auth.provided

import com.didit.application.auth.dto.EmailVerificationStartResult
import com.didit.application.auth.dto.SocialLoginCommand
import com.didit.application.auth.dto.SocialLoginResult

interface SocialAuth {
    fun login(command: SocialLoginCommand): SocialLoginResult

    fun startEmailVerification(
        loginSessionToken: String,
        email: String,
    ): EmailVerificationStartResult

    fun verifyEmail(
        loginSessionToken: String,
        code: String,
    ): SocialLoginResult
}
