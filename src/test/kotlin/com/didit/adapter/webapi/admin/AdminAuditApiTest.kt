package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminAuditFinder
import com.didit.application.admin.provided.AdminAuditLogItem
import com.didit.application.admin.provided.AdminAuditLogsResult
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class AdminAuditApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminAuditFinder: AdminAuditFinder = mock(AdminAuditFinder::class.java)

    override fun initController() = AdminAuditApi(adminAuditFinder)

    @Test
    fun `감사 로그 목록 조회`() {
        val result =
            AdminAuditLogsResult(
                content =
                    listOf(
                        AdminAuditLogItem(
                            action = "ADMIN_INVITED",
                            actionLabel = "관리자 초대",
                            actorId = UUID.randomUUID(),
                            actorType = "ADMIN",
                            targetId = UUID.randomUUID(),
                            targetType = "ADMIN",
                            payload = mapOf("email" to "new@example.com", "position" to "DEVELOPER"),
                            createdAt = LocalDateTime.of(2026, 6, 1, 12, 0),
                        ),
                    ),
                totalElements = 1,
                totalPages = 1,
                page = 0,
            )
        whenever(adminAuditFinder.findAuditLogs(any(), any(), any())).thenReturn(result)

        mockMvc
            .perform(
                get("/api/v1/admin/audit-logs")
                    .param("action", "ADMIN_INVITED")
                    .param("actorType", "ADMIN")
                    .param("page", "0")
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/audit-logs",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("action").optional().description("액션 필터 (ADMIN_INVITED, ADMIN_APPROVED 등)"),
                        parameterWithName("actorType").optional().description("액터 유형 (USER, ADMIN, SYSTEM)"),
                        parameterWithName("page").optional().description("페이지 번호 (기본값 0)"),
                    ),
                    responseFields(
                        fieldWithPath("data.content[].action").type(JsonFieldType.STRING).description("액션 (enum 이름)"),
                        fieldWithPath("data.content[].actionLabel").type(JsonFieldType.STRING).description("액션 한글 라벨"),
                        fieldWithPath("data.content[].actorId").type(JsonFieldType.STRING).optional().description("액터 ID"),
                        fieldWithPath("data.content[].actorType").type(JsonFieldType.STRING).optional().description("액터 유형"),
                        fieldWithPath("data.content[].targetId").type(JsonFieldType.STRING).optional().description("대상 ID"),
                        fieldWithPath("data.content[].targetType").type(JsonFieldType.STRING).optional().description("대상 유형"),
                        fieldWithPath("data.content[].payload").type(JsonFieldType.OBJECT).optional().description("상세 데이터"),
                        fieldWithPath("data.content[].payload.email").type(JsonFieldType.STRING).optional().description("예시 payload 필드"),
                        fieldWithPath("data.content[].payload.position").type(JsonFieldType.STRING).optional().description("예시 payload 필드"),
                        fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("발생 일시"),
                        fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 건수"),
                        fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                        fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                    ),
                ),
            )
    }
}
