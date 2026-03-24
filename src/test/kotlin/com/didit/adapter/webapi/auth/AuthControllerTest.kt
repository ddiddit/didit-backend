package com.didit.adapter.webapi.auth

import com.didit.adapter.auth.security.CustomUserDetails
import com.didit.application.auth.dto.RefreshTokenRequest
import com.didit.application.auth.dto.SocialLoginRequest
import com.didit.application.auth.dto.TokenInfo
import com.didit.application.auth.provided.LogoutUseCase
import com.didit.application.auth.provided.RefreshTokenUseCase
import com.didit.application.auth.provided.SocialLoginUseCase
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.auth.enums.Role
import com.didit.domain.auth.enums.SocialProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.Test

@WebMvcTest(AuthController::class)
@AutoConfigureRestDocs
@Import(AuthControllerTestConfig::class)
class AuthControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val socialLoginUseCase: SocialLoginUseCase,
    @Autowired val refreshTokenUseCase: RefreshTokenUseCase,
    @Autowired val logoutUseCase: LogoutUseCase,
) {
    @Test
    fun `소셜_로그인_성공`() {
        val request =
            SocialLoginRequest(
                provider = SocialProvider.GOOGLE,
                idToken = "test-id-token",
            )

        val tokenInfo =
            TokenInfo(
                accessToken = "access-token",
                refreshToken = "refresh-token",
            )

        whenever(socialLoginUseCase.login(eq(request.provider), any())).thenReturn(tokenInfo)

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth-login",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("provider").type(JsonFieldType.STRING).description("소셜 로그인 제공자 (GOOGLE)"),
                        fieldWithPath("idToken").type(JsonFieldType.STRING).description("OIDC ID 토큰"),
                    ),
                    responseFields(
                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                    ),
                ),
            )
    }

    @Test
    fun `토큰_재발급_성공`() {
        val request = RefreshTokenRequest("refresh-token")

        val tokenInfo =
            TokenInfo(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
            )

        whenever(refreshTokenUseCase.refresh(any())).thenReturn(tokenInfo)

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth-refresh",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                    ),
                    responseFields(
                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                    ),
                ),
            )
    }

    @Test
    fun `카카오_콜백_로그인_성공`() {
        val code = "test-auth-code"

        val tokenInfo =
            TokenInfo(
                accessToken = "access-token",
                refreshToken = "refresh-token",
            )

        whenever(
            socialLoginUseCase.loginWithKakao(
                eq(code),
                eq("http://localhost:8080/api/v1/auth/kakao/callback"),
            ),
        ).thenReturn(tokenInfo)

        mockMvc
            .perform(
                get("/api/v1/auth/kakao/callback")
                    .param("code", code),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth-kakao-callback",
                    responseFields(
                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                    ),
                ),
            )
    }

    @Test
    fun `로그아웃_성공`() {
        val user =
            CustomUserDetails(
                userId = UUID.randomUUID(),
                role = Role.USER,
            )

        doNothing().`when`(logoutUseCase).logout(any())

        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .with(csrf())
                    .with(user(user)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
            .andDo(
                document(
                    "auth-logout",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data")
                            .type(JsonFieldType.NULL)
                            .description("응답 데이터 (로그아웃 성공 시 null 반환)"),
                    ),
                ),
            )
    }
}
