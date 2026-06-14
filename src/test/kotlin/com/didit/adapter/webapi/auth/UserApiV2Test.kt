package com.didit.adapter.webapi.auth

import com.didit.application.achievement.provided.BadgeFinder
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.provided.UserRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import com.didit.support.UserFixture
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserApiV2Test : AuthenticatedRestDocsSupport() {
    private val userFinder: UserFinder = mock(UserFinder::class.java)
    private val userRegister: UserRegister = mock(UserRegister::class.java)
    private val badgeFinder: BadgeFinder = mock(BadgeFinder::class.java)

    override fun initController() = UserApiV2(userFinder, userRegister, badgeFinder)

    @Test
    fun `온보딩 완료 v2`() {
        val request =
            mapOf(
                "nickname" to "디딧유저",
                "job" to Job.DEVELOPER,
                "age" to UserAge.AGE_20,
                "experience" to UserExperience.YEARS_1_TO_2,
                "marketingAgreed" to true,
                "nightPushAgreed" to false,
            )

        mockMvc
            .perform(
                post("/api/v2/users/onboarding")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "user/v2/onboarding",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("job")
                            .type(JsonFieldType.STRING)
                            .description("직무 (DEVELOPER, PLANNER, DESIGNER)"),
                        fieldWithPath("age")
                            .type(JsonFieldType.STRING)
                            .description("나이대 (AGE_20, AGE_30, AGE_40_PLUS)")
                            .optional(),
                        fieldWithPath("experience")
                            .type(JsonFieldType.STRING)
                            .description("경력 (LESS_THAN_1_YEAR, YEARS_1_TO_2, YEARS_3_TO_5, YEARS_6_TO_9, YEARS_10_PLUS)")
                            .optional(),
                        fieldWithPath("marketingAgreed").type(JsonFieldType.BOOLEAN).description("마케팅 정보 수신 동의"),
                        fieldWithPath("nightPushAgreed").type(JsonFieldType.BOOLEAN).description("야간 푸시 수신 동의"),
                    ),
                ),
            )
    }

    @Test
    fun `프로필 조회 v2`() {
        val user = UserFixture.createOnboarded()
        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(badgeFinder.findRecent(userId)).thenReturn(emptyList())

        mockMvc
            .perform(get("/api/v2/users/profile"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "user/v2/profile",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("data.job")
                            .type(JsonFieldType.STRING)
                            .description("직무 (DEVELOPER, PLANNER, DESIGNER)"),
                        fieldWithPath("data.age")
                            .type(JsonFieldType.STRING)
                            .description("나이대 (AGE_20, AGE_30, AGE_40_PLUS)")
                            .optional(),
                        fieldWithPath("data.experience")
                            .type(JsonFieldType.STRING)
                            .description("경력 (LESS_THAN_1_YEAR, YEARS_1_TO_2, YEARS_3_TO_5, YEARS_6_TO_9, YEARS_10_PLUS)")
                            .optional(),
                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                        fieldWithPath("data.provider")
                            .type(JsonFieldType.STRING)
                            .description("소셜 로그인 제공자 (KAKAO, GOOGLE, APPLE)"),
                        fieldWithPath("data.recentBadges").type(JsonFieldType.ARRAY).description("최근 획득 배지 목록 (최대 3개)"),
                    ),
                ),
            )
    }

    @Test
    fun `프로필 수정 v2`() {
        val request =
            mapOf(
                "nickname" to "새닉네임",
                "job" to Job.DESIGNER,
                "age" to UserAge.AGE_30,
                "experience" to UserExperience.YEARS_3_TO_5,
            )

        mockMvc
            .perform(
                patch("/api/v2/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "user/v2/update-profile",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("job")
                            .type(JsonFieldType.STRING)
                            .description("직무 (DEVELOPER, PLANNER, DESIGNER)"),
                        fieldWithPath("age")
                            .type(JsonFieldType.STRING)
                            .description("나이대 (AGE_20, AGE_30, AGE_40_PLUS)")
                            .optional(),
                        fieldWithPath("experience")
                            .type(JsonFieldType.STRING)
                            .description("경력 (LESS_THAN_1_YEAR, YEARS_1_TO_2, YEARS_3_TO_5, YEARS_6_TO_9, YEARS_10_PLUS)")
                            .optional(),
                    ),
                ),
            )
    }
}
