package com.didit.adapter.webapi.auth

import com.didit.application.auth.dto.RefreshResponse
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.provided.Auth
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.auth.Provider
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthApiTest : AuthenticatedRestDocsSupport() {
    private val auth: Auth = mock(Auth::class.java)

    override fun initController() = AuthApi(auth)

    @Test
    fun `소셜 로그인`() {
        val request =
            mapOf(
                "provider" to Provider.KAKAO,
                "oauthToken" to "kakao-oauth-token",
            )
        val response =
            TokenResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                isNewUser = true,
                isOnboardingCompleted = false,
            )
        whenever(auth.login(any(), any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth/login",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("provider").type(JsonFieldType.STRING).description("소셜 로그인 제공자 (KAKAO, GOOGLE, APPLE)"),
                        fieldWithPath("oauthToken").type(JsonFieldType.STRING).description("소셜 로그인 토큰"),
                    ),
                    responseFields(
                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                        fieldWithPath("data.isNewUser").type(JsonFieldType.BOOLEAN).description("신규 유저 여부"),
                        fieldWithPath("data.isOnboardingCompleted").type(JsonFieldType.BOOLEAN).description("온보딩 완료 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `토큰 재발급`() {
        val request = mapOf("refreshToken" to "refresh-token")
        val response =
            RefreshResponse(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
            )
        whenever(auth.refresh(any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "auth/refresh",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                    ),
                    responseFields(
                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새 액세스 토큰"),
                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("새 리프레시 토큰"),
                    ),
                ),
            )
    }

    @Test
    fun `로그아웃`() {
        mockMvc
            .perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "auth/logout",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                ),
            )
    }

    @Test
    fun `회원 탈퇴`() {
        val request =
            mapOf(
                "reason" to "NO_LONGER_NEEDED",
            )

        mockMvc
            .perform(
                delete("/api/v1/auth/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "auth/withdraw",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("reason")
                            .type(JsonFieldType.STRING)
                            .description(
                                "탈퇴 사유 (NO_LONGER_NEEDED, MISSING_FEATURES, SERVICE_ISSUES, DIFFICULT_TO_USE, SWITCHING_SERVICE, OTHER)",
                            ),
                        fieldWithPath("reasonDetail")
                            .type(JsonFieldType.STRING)
                            .description("기타 사유 상세 내용 (OTHER 선택 시 필수)")
                            .optional(),
                    ),
                ),
            )
    }
}
