package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminUserFinder
import com.didit.application.admin.provided.AdminUserManager
import com.didit.application.admin.provided.UserDetailResult
import com.didit.application.admin.provided.UserListResult
import com.didit.application.admin.provided.UserSummary
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditEntry
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.auth.Provider
import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class UserManagementApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminUserFinder: AdminUserFinder = mock(AdminUserFinder::class.java)
    private val adminUserManager: AdminUserManager = mock(AdminUserManager::class.java)

    override fun initController() = UserManagementApi(adminUserFinder, adminUserManager)

    private fun createUserSummary(
        id: UUID = UUID.randomUUID(),
        deleted: Boolean = false,
    ) = UserSummary(
        id = id,
        email = "user@example.com",
        nickname = "디딧유저",
        job = Job.DEVELOPER,
        age = UserAge.AGE_30,
        experience = UserExperience.LESS_THAN_1_YEAR,
        provider = Provider.KAKAO.name,
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        lastLoginAt = LocalDateTime.of(2024, 6, 1, 12, 0, 0),
        onboardingCompleted = true,
        deleted = deleted,
    )

    @Test
    fun `유저 목록 조회`() {
        val result =
            UserListResult(
                content = listOf(createUserSummary(), createUserSummary()),
                page = 0,
                size = 20,
                totalElements = 2,
                totalPages = 1,
            )

        whenever(adminUserFinder.findUsers(any(), any(), any(), any())).thenReturn(result)

        mockMvc
            .perform(
                get("/api/v1/admin/users")
                    .param("keyword", "디딧")
                    .param("job", "DEVELOPER")
                    .param("isDeleted", "false")
                    .param("page", "0")
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "user/admin/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("keyword").optional().description("검색어 (이메일, 닉네임)"),
                        parameterWithName("job").optional().description("직무 필터 (DEVELOPER, DESIGNER, PLANNER, MARKETER, ETC)"),
                        parameterWithName("isDeleted").optional().description("탈퇴 여부 필터"),
                        parameterWithName("page").optional().description("페이지 번호 (기본값 0)"),
                    ),
                    responseFields(
                        fieldWithPath("data.content[].id").type(JsonFieldType.STRING).description("유저 ID"),
                        fieldWithPath("data.content[].email").type(JsonFieldType.STRING).optional().description("이메일"),
                        fieldWithPath("data.content[].nickname").type(JsonFieldType.STRING).optional().description("닉네임"),
                        fieldWithPath("data.content[].job")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("직무 (DEVELOPER, DESIGNER, PLANNER, MARKETER, ETC)"),
                        fieldWithPath("data.content[].age")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("연령대 (TEENS, TWENTIES, THIRTIES, FORTIES, FIFTIES_PLUS)"),
                        fieldWithPath("data.content[].experience")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("연차 (JUNIOR, MID, SENIOR)"),
                        fieldWithPath("data.content[].provider")
                            .type(JsonFieldType.STRING)
                            .description("소셜 로그인 제공자 (KAKAO, GOOGLE, APPLE)"),
                        fieldWithPath("data.content[].createdAt")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("가입 일시"),
                        fieldWithPath("data.content[].lastLoginAt")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("마지막 로그인 일시"),
                        fieldWithPath("data.content[].onboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("온보딩 완료 여부"),
                        fieldWithPath("data.content[].deleted").type(JsonFieldType.BOOLEAN).description("탈퇴 여부"),
                        fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                        fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                        fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 유저 수"),
                        fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                    ),
                ),
            )
    }

    @Test
    fun `유저 상세 조회`() {
        val userId = UUID.randomUUID()
        val summary = createUserSummary(id = userId)
        val timeline =
            listOf(
                AuditEntry(
                    action = AuditAction.USER_LOGGED_IN,
                    payload = null,
                    createdAt = LocalDateTime.of(2024, 6, 1, 12, 0, 0),
                ),
                AuditEntry(
                    action = AuditAction.RETROSPECTIVE_STARTED,
                    payload = null,
                    createdAt = LocalDateTime.of(2024, 5, 1, 10, 0, 0),
                ),
            )

        val result = UserDetailResult(profile = summary, timeline = timeline)

        whenever(adminUserFinder.findUserDetail(any())).thenReturn(result)

        mockMvc
            .perform(
                get("/api/v1/admin/users/{userId}", userId)
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "user/admin/detail",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("userId").description("유저 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.profile.id").type(JsonFieldType.STRING).description("유저 ID"),
                        fieldWithPath("data.profile.email").type(JsonFieldType.STRING).optional().description("이메일"),
                        fieldWithPath("data.profile.nickname")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("닉네임"),
                        fieldWithPath("data.profile.job")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("직무"),
                        fieldWithPath("data.profile.age")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("연령대"),
                        fieldWithPath("data.profile.experience")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("연차"),
                        fieldWithPath("data.profile.provider")
                            .type(JsonFieldType.STRING)
                            .description("소셜 로그인 제공자"),
                        fieldWithPath("data.profile.createdAt")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("가입 일시"),
                        fieldWithPath("data.profile.lastLoginAt")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("마지막 로그인 일시"),
                        fieldWithPath("data.profile.onboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("온보딩 완료 여부"),
                        fieldWithPath("data.profile.deleted").type(JsonFieldType.BOOLEAN).description("탈퇴 여부"),
                        fieldWithPath("data.timeline[].action")
                            .type(JsonFieldType.STRING)
                            .description("활동 유형"),
                        fieldWithPath("data.timeline[].payload")
                            .type(JsonFieldType.VARIES)
                            .optional()
                            .description("활동 상세 정보 (nullable)"),
                        fieldWithPath("data.timeline[].createdAt")
                            .type(JsonFieldType.STRING)
                            .description("활동 일시"),
                    ),
                ),
            )
    }

    @Test
    fun `유저 강제 탈퇴`() {
        val userId = UUID.randomUUID()

        mockMvc
            .perform(
                post("/api/v1/admin/users/{userId}/force-withdraw", userId)
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "user/admin/force-withdraw",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("userId").description("강제 탈퇴할 유저 ID"),
                    ),
                ),
            )
    }
}
