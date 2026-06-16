package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminBadgeFinder
import com.didit.application.admin.provided.AdminBadgeHolder
import com.didit.application.admin.provided.AdminBadgeResult
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class AdminBadgeApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminBadgeFinder: AdminBadgeFinder = mock(AdminBadgeFinder::class.java)

    override fun initController() = AdminBadgeApi(adminBadgeFinder)

    @Test
    fun `배지 목록 조회`() {
        val result = listOf(
            AdminBadgeResult(
                id = UUID.randomUUID(),
                name = "첫 회고",
                description = "첫 번째 회고를 완료했어요",
                conditionType = "RETROSPECT_COUNT",
                acquiredCount = 150,
                createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            ),
        )
        whenever(adminBadgeFinder.findAll()).thenReturn(result)

        mockMvc
            .perform(get("/api/v1/admin/badges").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("배지 ID"),
                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("배지명"),
                        fieldWithPath("data[].description").type(JsonFieldType.STRING).description("배지 설명"),
                        fieldWithPath("data[].conditionType").type(JsonFieldType.STRING).description("획득 조건 유형"),
                        fieldWithPath("data[].acquiredCount").type(JsonFieldType.NUMBER).description("획득 유저 수"),
                        fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).optional().description("생성 일시"),
                    ),
                ),
            )
    }

    @Test
    fun `배지 보유 유저 목록 조회`() {
        val badgeId = UUID.randomUUID()
        val result = listOf(
            AdminBadgeHolder(
                userId = UUID.randomUUID(),
                email = "user@example.com",
                nickname = "디딧유저",
                acquiredAt = LocalDateTime.of(2026, 6, 1, 12, 0),
            ),
        )
        whenever(adminBadgeFinder.findHolders(any())).thenReturn(result)

        mockMvc
            .perform(
                get("/api/v1/admin/badges/{badgeId}/holders", badgeId)
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/holders",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("badgeId").description("배지 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data[].userId").type(JsonFieldType.STRING).description("유저 ID"),
                        fieldWithPath("data[].email").type(JsonFieldType.STRING).optional().description("이메일"),
                        fieldWithPath("data[].nickname").type(JsonFieldType.STRING).optional().description("닉네임"),
                        fieldWithPath("data[].acquiredAt").type(JsonFieldType.STRING).description("획득 일시"),
                    ),
                ),
            )
    }
}
