package com.didit.adapter.webapi.auth

import com.didit.adapter.webapi.auth.dto.EmailVerificationStartRequest
import com.didit.adapter.webapi.auth.dto.EmailVerificationStartResponse
import com.didit.adapter.webapi.auth.dto.EmailVerificationVerifyRequest
import com.didit.adapter.webapi.auth.dto.SocialLoginRequest
import com.didit.adapter.webapi.auth.dto.SocialLoginResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.auth.provided.SocialAuth
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v2/auth/social")
@RestController
class SocialAuthApi(
    private val socialAuth: SocialAuth,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: SocialLoginRequest,
    ): SuccessResponse<SocialLoginResponse> =
        SuccessResponse.of(
            SocialLoginResponse.from(
                socialAuth.login(request.provider, request.credentialType, request.credential, request.redirectUri),
            ),
        )

    @PostMapping("/email/start")
    fun startEmailVerification(
        @Valid @RequestBody request: EmailVerificationStartRequest,
    ): SuccessResponse<EmailVerificationStartResponse> {
        val result = socialAuth.startEmailVerification(request.loginSessionToken, request.email)
        return SuccessResponse.of(EmailVerificationStartResponse(result.expiresInSeconds))
    }

    @PostMapping("/email/verify")
    fun verifyEmail(
        @Valid @RequestBody request: EmailVerificationVerifyRequest,
    ): SuccessResponse<SocialLoginResponse> =
        SuccessResponse.of(
            SocialLoginResponse.from(
                socialAuth.verifyEmail(request.loginSessionToken, request.code),
            ),
        )
}
