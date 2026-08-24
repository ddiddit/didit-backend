package com.didit.adapter.webapi.auth

import com.didit.application.auth.dto.EmailVerificationStartResult
import com.didit.application.auth.dto.SocialLoginResult
import com.didit.application.auth.dto.SocialLoginStatus
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.provided.SocialAuth
import com.didit.docs.RestDocsSupport
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialCredentialType
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class SocialAuthApiTest : RestDocsSupport() {
    private val socialAuth: SocialAuth = mock(SocialAuth::class.java)

    override fun initController() = SocialAuthApi(socialAuth)

    @Test
    fun `모르는 식별자의 로그인 응답은 이메일 인증 세션만 반환한다`() {
        whenever(socialAuth.login(any(), any(), any())).thenReturn(
            SocialLoginResult(
                status = SocialLoginStatus.EMAIL_VERIFICATION_REQUIRED,
                loginSessionToken = "login-session-token",
                emailHint = "m***@example.com",
            ),
        )

        mockMvc
            .post("/api/v2/auth/social/login") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "provider" to Provider.KAKAO,
                            "credentialType" to SocialCredentialType.AUTHORIZATION_CODE,
                            "credential" to "authorization-code",
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("EMAIL_VERIFICATION_REQUIRED") }
                jsonPath("$.data.loginSessionToken") { value("login-session-token") }
                jsonPath("$.data.accessToken") { value(nullValue()) }
            }
    }

    @Test
    fun `이메일 인증번호 발송 요청은 만료 초를 반환한다`() {
        whenever(socialAuth.startEmailVerification(any(), any())).thenReturn(EmailVerificationStartResult(600))

        mockMvc
            .post("/api/v2/auth/social/email/start") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "loginSessionToken" to "login-session-token",
                            "email" to "member@example.com",
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.expiresInSeconds") { value(600) }
            }
    }

    @Test
    fun `이메일 인증 완료 후에는 서버 판정 결과와 서비스 토큰을 반환한다`() {
        whenever(socialAuth.verifyEmail(any(), any())).thenReturn(
            SocialLoginResult(
                status = SocialLoginStatus.AUTHENTICATED,
                token =
                    TokenResponse(
                        accessToken = "access-token",
                        refreshToken = "refresh-token",
                        isNewUser = false,
                        isOnboardingCompleted = true,
                    ),
            ),
        )

        mockMvc
            .post("/api/v2/auth/social/email/verify") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "loginSessionToken" to "login-session-token",
                            "code" to "123456",
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("AUTHENTICATED") }
                jsonPath("$.data.accessToken") { value("access-token") }
                jsonPath("$.data.isNewUser") { value(false) }
                jsonPath("$.data.isOnboardingCompleted") { value(true) }
            }
    }
}
