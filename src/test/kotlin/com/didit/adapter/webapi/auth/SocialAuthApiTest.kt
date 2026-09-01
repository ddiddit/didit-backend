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
import org.mockito.kotlin.anyOrNull
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
        whenever(socialAuth.login(any(), any(), any(), anyOrNull())).thenReturn(
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
                            "provider" to Provider.GOOGLE,
                            "credentialType" to SocialCredentialType.ID_TOKEN,
                            "credential" to "id-token",
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
                            fieldWithPath("provider").type(JsonFieldType.STRING).description("소셜 로그인 제공자 (KAKAO, GOOGLE, APPLE)"),
                            fieldWithPath(
                                "credentialType",
                            ).type(JsonFieldType.STRING).description("제공자에 맞는 인증값 유형 (ID_TOKEN, ACCESS_TOKEN, AUTHORIZATION_CODE)"),
                            fieldWithPath("credential").type(JsonFieldType.STRING).description("credentialType에 맞는 소셜 인증값"),
                            fieldWithPath(
                                "redirectUri",
                            ).type(JsonFieldType.STRING).description("인가 코드 교환에 사용할 callback URI (필요한 제공자에 한함)").optional(),
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
            }.andDo {
                handle(
                    document(
                        "auth/social-login-v2/email-start",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestFields(
                            fieldWithPath("loginSessionToken").type(JsonFieldType.STRING).description("소셜 로그인 세션 토큰"),
                            fieldWithPath("email").type(JsonFieldType.STRING).description("인증번호를 받을 이메일 주소"),
                        ),
                        responseFields(
                            fieldWithPath("data.expiresInSeconds").type(JsonFieldType.NUMBER).description("인증번호 만료까지 남은 초"),
                        ),
                    ),
                )
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
            }.andDo {
                handle(
                    document(
                        "auth/social-login-v2/email-verify",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestFields(
                            fieldWithPath("loginSessionToken").type(JsonFieldType.STRING).description("소셜 로그인 세션 토큰"),
                            fieldWithPath("code").type(JsonFieldType.STRING).description("이메일 인증번호"),
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
}
