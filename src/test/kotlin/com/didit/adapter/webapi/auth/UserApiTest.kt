package com.didit.adapter.webapi.auth

import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.provided.UserRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.auth.Job
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserApiTest : AuthenticatedRestDocsSupport() {
    private val userFinder: UserFinder = mock(UserFinder::class.java)
    private val userRegister: UserRegister = mock(UserRegister::class.java)

    override fun initController() = UserApi(userFinder, userRegister)

    @Test
    fun `닉네임 중복 확인`() {
        whenever(userFinder.existsByNickname("디딧유저")).thenReturn(false)

        mockMvc
            .perform(
                get("/api/v1/users/nickname/check")
                    .param("nickname", "디딧유저"),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "user/nickname-check",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("nickname").description("확인할 닉네임"),
                    ),
                    responseFields(
                        fieldWithPath("data.isDuplicate").type(JsonFieldType.BOOLEAN).description("닉네임 중복 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `온보딩 완료`() {
        val request =
            mapOf(
                "nickname" to "디딧유저",
                "job" to Job.DEVELOPER,
                "marketingAgreed" to true,
                "nightPushAgreed" to false,
            )

        mockMvc
            .perform(
                post("/api/v1/users/onboarding")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "user/onboarding",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("job").type(JsonFieldType.STRING).description("직무 (DEVELOPER, PLANNER, DESIGNER)"),
                        fieldWithPath("marketingAgreed").type(JsonFieldType.BOOLEAN).description("마케팅 정보 수신 동의"),
                        fieldWithPath("nightPushAgreed").type(JsonFieldType.BOOLEAN).description("야간 푸시 수신 동의"),
                    ),
                ),
            )
    }

    @Test
    fun `프로필 수정`() {
        val request =
            mapOf(
                "nickname" to "새닉네임",
                "job" to Job.DESIGNER,
            )

        mockMvc
            .perform(
                patch("/api/v1/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "user/update-profile",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("job").type(JsonFieldType.STRING).description("직무 (DEVELOPER, PLANNER, DESIGNER)"),
                    ),
                ),
            )
    }

    @Test
    fun `마케팅 정보 수신 동의 수정`() {
        val request = mapOf("agreed" to true)

        mockMvc
            .perform(
                patch("/api/v1/users/marketing-consent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "user/marketing-consent",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("agreed").type(JsonFieldType.BOOLEAN).description("마케팅 정보 수신 동의 여부"),
                    ),
                ),
            )
    }
}
