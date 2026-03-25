package com.didit.adapter.webapi.auth

import com.didit.adapter.webapi.exception.ApiControllerAdvice
import com.didit.adapter.webapi.resolver.CurrentUserIdResolver
import com.didit.application.auth.dto.RefreshResponse
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.provided.Auth
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.RestDocsSupport
import com.didit.domain.auth.Provider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.util.UUID

class AuthApiTest : RestDocsSupport() {
    private val auth: Auth = mock(Auth::class.java)
    private val userId = UUID.randomUUID()

    override fun initController() = AuthApi(auth)

    @BeforeEach
    fun setUpSecurityContext(provider: RestDocumentationContextProvider) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(initController())
                .setControllerAdvice(ApiControllerAdvice())
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(LocalValidatorFactoryBean().also { it.afterPropertiesSet() })
                .setCustomArgumentResolvers(CurrentUserIdResolver())
                .apply<StandaloneMockMvcBuilder>(documentationConfiguration(provider))
                .build()
    }

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
        mockMvc
            .perform(delete("/api/v1/auth/withdraw"))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "auth/withdraw",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                ),
            )
    }
}
