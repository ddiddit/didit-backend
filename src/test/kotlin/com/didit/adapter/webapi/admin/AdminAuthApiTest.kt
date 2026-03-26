package com.didit.adapter.webapi.admin

import com.didit.application.admin.dto.AdminRefreshResponse
import com.didit.application.admin.dto.AdminTokenResponse
import com.didit.application.admin.provided.AdminAuth
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminAuthApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminAuth: AdminAuth = mock(AdminAuth::class.java)

    override fun initController() = AdminAuthApi(adminAuth)

    @Test
    fun `어드민 로그인`() {
        val request =
            mapOf(
                "email" to "admin@didit.com",
                "password" to "password123!",
            )
        val response =
            AdminTokenResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
            )
        whenever(adminAuth.login(any(), any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/auth/login",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                        fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
                    ),
                    responseFields(
                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                    ),
                ),
            )
    }

    @Test
    fun `어드민 토큰 재발급`() {
        val request = mapOf("refreshToken" to "refresh-token")
        val response =
            AdminRefreshResponse(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
            )
        whenever(adminAuth.refresh(any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/admin/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/auth/refresh",
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
    fun `어드민 로그아웃`() {
        mockMvc
            .perform(post("/api/v1/admin/auth/logout"))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin/auth/logout",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                ),
            )
    }
}
