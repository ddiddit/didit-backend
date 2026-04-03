package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminFinder
import com.didit.application.admin.provided.AdminInviteManager
import com.didit.application.admin.provided.AdminManager
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.admin.AdminPosition
import com.didit.support.AdminFixture
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class AdminApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminFinder: AdminFinder = mock(AdminFinder::class.java)
    private val adminManager: AdminManager = mock(AdminManager::class.java)
    private val adminInviteManager: AdminInviteManager = mock(AdminInviteManager::class.java)

    override fun initController() = AdminApi(adminFinder, adminManager, adminInviteManager)

    @Test
    fun `관리자 초대`() {
        val request =
            mapOf(
                "email" to "invite@didit.com",
                "position" to AdminPosition.DEVELOPER,
            )

        mockMvc
            .perform(
                post("/api/v1/admin/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin/invite",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("email").type(JsonFieldType.STRING).description("초대할 이메일"),
                        fieldWithPath("position").type(JsonFieldType.STRING).description("직군 (PLANNER, DESIGNER, DEVELOPER)"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 계정 생성`() {
        val request =
            mapOf(
                "email" to "invite@didit.com",
                "password" to "password123!",
            )

        mockMvc
            .perform(
                post("/api/v1/admin/register")
                    .param("token", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin/register",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("token").description("초대 토큰"),
                    ),
                    requestFields(
                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                        fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 목록 조회`() {
        val admins =
            listOf(
                AdminFixture.createAdmin(),
                AdminFixture.createAdmin(email = "admin2@didit.com", position = AdminPosition.PLANNER),
            )
        whenever(adminFinder.findAll()).thenReturn(admins)

        mockMvc
            .perform(get("/api/v1/admin"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("관리자 ID"),
                        fieldWithPath("data[].email").type(JsonFieldType.STRING).description("이메일"),
                        fieldWithPath("data[].role").type(JsonFieldType.STRING).description("권한 (SUPER_ADMIN, ADMIN)"),
                        fieldWithPath(
                            "data[].position",
                        ).type(JsonFieldType.STRING).optional().description("직군 (PLANNER, DESIGNER, DEVELOPER)"),
                        fieldWithPath("data[].status").type(JsonFieldType.STRING).description("상태 (PENDING, ACTIVE, REJECTED)"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 승인`() {
        val targetAdminId = UUID.randomUUID()

        mockMvc
            .perform(post("/api/v1/admin/{adminId}/approve", targetAdminId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin/approve",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("adminId").description("승인할 관리자 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 거절`() {
        val targetAdminId = UUID.randomUUID()

        mockMvc
            .perform(post("/api/v1/admin/{adminId}/reject", targetAdminId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin/reject",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("adminId").description("거절할 관리자 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 삭제`() {
        val targetAdminId = UUID.randomUUID()

        mockMvc
            .perform(delete("/api/v1/admin/{adminId}", targetAdminId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin/delete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("adminId").description("삭제할 관리자 ID"),
                    ),
                ),
            )
    }
}
