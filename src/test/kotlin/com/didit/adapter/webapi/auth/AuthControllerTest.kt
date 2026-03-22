package com.didit.adapter.webapi.auth

import com.didit.application.auth.dto.RefreshTokenRequest
import com.didit.application.auth.dto.SocialLoginRequest
import com.didit.application.auth.dto.TokenInfo
import com.didit.application.auth.provided.RefreshTokenUseCase
import com.didit.application.auth.provided.SocialLoginUseCase
import com.didit.domain.auth.enums.SocialProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(AuthController::class)
@AutoConfigureRestDocs
@Import(AuthControllerTestConfig::class)
class AuthControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val socialLoginUseCase: SocialLoginUseCase,
    @Autowired val refreshTokenUseCase: RefreshTokenUseCase,
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
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth-social-login",
                    requestFields(
                        fieldWithPath("provider").description("소셜 로그인 제공자 (GOOGLE)"),
                        fieldWithPath("idToken").description("OIDC ID 토큰"),
                    ),
                    responseFields(
                        fieldWithPath("data.accessToken").description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").description("리프레시 토큰"),
                        fieldWithPath("message").description("응답 메시지"),
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
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth-refresh",
                    requestFields(
                        fieldWithPath("refreshToken").description("리프레시 토큰"),
                    ),
                    responseFields(
                        fieldWithPath("data.accessToken").description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").description("리프레시 토큰"),
                        fieldWithPath("message").description("응답 메시지"),
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
                any(),
            ),
        ).thenReturn(tokenInfo)

        mockMvc
            .perform(
                get("/auth/social/kakao")
                    .param("code", code),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth-kakao-login",
                    responseFields(
                        fieldWithPath("data.accessToken").description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").description("리프레시 토큰"),
                        fieldWithPath("message").description("응답 메시지"),
                    ),
                ),
            )
    }
}
