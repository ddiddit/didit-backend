package com.didit.adapter.webapi.auth

import com.didit.application.auth.dto.EmailVerificationStartResult
import com.didit.application.auth.dto.SocialLoginResult
import com.didit.application.auth.dto.SocialLoginStatus
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.provided.SocialAuth
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.RestDocsSupport
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialCredentialType
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.post

class SocialAuthApiTest : RestDocsSupport() {
    private val socialAuth: SocialAuth = mock(SocialAuth::class.java)

    override fun initController() = SocialAuthApi(socialAuth)

    @Test
    fun `소셜 로그인 v2`() {
        whenever(socialAuth.login(any(), any(), any(), any())).thenReturn(
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
                            "redirectUri" to "http://localhost:3000/auth/kakao/callback",
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("EMAIL_VERIFICATION_REQUIRED") }
                jsonPath("$.data.loginSessionToken") { value("login-session-token") }
                jsonPath("$.data.accessToken") { value(nullValue()) }
            }.andDo {
                handle(
                    document(
                        "auth/social-login-v2",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestFields(
                            fieldWithPath("provider").type(JsonFieldType.STRING).description("소셜 로그인 제공자"),
                            fieldWithPath("credentialType").type(JsonFieldType.STRING).description("Kakao 인가 코드는 AUTHORIZATION_CODE"),
                            fieldWithPath("credential").type(JsonFieldType.STRING).description("소셜 로그인 credential"),
                            fieldWithPath(
                                "redirectUri",
                            ).type(JsonFieldType.STRING).description("Kakao 인가 코드 발급에 사용한 callback URI").optional(),
                        ),
                        responseFields(
                            fieldWithPath("data.status").type(JsonFieldType.STRING).description("로그인 처리 상태"),
                            fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰").optional(),
                            fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰").optional(),
                            fieldWithPath("data.isNewUser").type(JsonFieldType.BOOLEAN).description("신규 사용자 여부").optional(),
                            fieldWithPath("data.isOnboardingCompleted").type(JsonFieldType.BOOLEAN).description("온보딩 완료 여부").optional(),
                            fieldWithPath("data.loginSessionToken").type(JsonFieldType.STRING).description("이메일 인증용 로그인 세션 토큰").optional(),
                            fieldWithPath("data.emailHint").type(JsonFieldType.STRING).description("이메일 인증 안내용 마스킹 이메일").optional(),
                        ),
                    ),
                )
            }
    }

    @Test
    fun `소셜 로그인 v2 이메일 인증번호 발송`() {
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
    fun `소셜 로그인 v2 이메일 인증 완료`() {
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
