package com.didit.adapter.webapi.admin

import com.didit.application.admin.AdminPromptService
import com.didit.application.admin.provided.AdminPromptFinder
import com.didit.application.admin.provided.AdminPromptResult
import com.didit.application.audit.AuditLogger
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class AdminPromptApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminPromptFinder: AdminPromptFinder = mock(AdminPromptFinder::class.java)
    private val adminPromptService: AdminPromptService = mock(AdminPromptService::class.java)
    private val auditLogger: AuditLogger = mock(AuditLogger::class.java)

    override fun initController() = AdminPromptApi(adminPromptFinder, adminPromptService, auditLogger)

    private fun createPromptResult(
        jobType: String = "DEVELOPER",
        promptType: String = "DEEP_QUESTION",
    ) = AdminPromptResult(
        id = UUID.randomUUID(),
        jobType = jobType,
        promptType = promptType,
        content = "당신은 전문 회고 코치입니다. {{q1}} {{q2}} {{q3}}",
        updatedAt = LocalDateTime.of(2026, 6, 1, 0, 0),
        updatedBy = null,
    )

    @Test
    fun `프롬프트 목록 조회`() {
        val result =
            listOf(
                createPromptResult(jobType = "DEVELOPER", promptType = "DEEP_QUESTION"),
                createPromptResult(jobType = "DEVELOPER", promptType = "SUMMARY"),
                createPromptResult(jobType = "PLANNER", promptType = "DEEP_QUESTION"),
            )
        whenever(adminPromptFinder.findAll()).thenReturn(result)

        mockMvc
            .perform(get("/api/v1/admin/prompts").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/prompts/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("프롬프트 ID"),
                        fieldWithPath("data[].jobType").type(JsonFieldType.STRING).description("직군 (DEVELOPER, PLANNER, DESIGNER)"),
                        fieldWithPath("data[].promptType").type(JsonFieldType.STRING).description("프롬프트 유형 (DEEP_QUESTION, SUMMARY)"),
                        fieldWithPath("data[].content").type(JsonFieldType.STRING).description("프롬프트 내용"),
                        fieldWithPath("data[].updatedAt").type(JsonFieldType.STRING).description("마지막 수정 일시"),
                        fieldWithPath("data[].updatedBy").type(JsonFieldType.STRING).optional().description("마지막 수정자 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `프롬프트 단건 조회`() {
        val id = UUID.randomUUID()
        whenever(adminPromptFinder.findById(any())).thenReturn(createPromptResult())

        mockMvc
            .perform(get("/api/v1/admin/prompts/{id}", id).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/prompts/get",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("id").description("프롬프트 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("프롬프트 ID"),
                        fieldWithPath("data.jobType").type(JsonFieldType.STRING).description("직군"),
                        fieldWithPath("data.promptType").type(JsonFieldType.STRING).description("프롬프트 유형"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("프롬프트 내용"),
                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("마지막 수정 일시"),
                        fieldWithPath("data.updatedBy").type(JsonFieldType.STRING).optional().description("마지막 수정자 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `프롬프트 수정`() {
        val id = UUID.randomUUID()
        val request = mapOf("content" to "수정된 프롬프트 내용")
        whenever(adminPromptService.update(any(), any(), any())).thenReturn(
            createPromptResult().copy(content = "수정된 프롬프트 내용", updatedBy = adminId.toString()),
        )

        mockMvc
            .perform(
                put("/api/v1/admin/prompts/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/prompts/update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("id").description("프롬프트 ID"),
                    ),
                    requestFields(
                        fieldWithPath("content").type(JsonFieldType.STRING).description("수정할 프롬프트 내용"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("프롬프트 ID"),
                        fieldWithPath("data.jobType").type(JsonFieldType.STRING).description("직군"),
                        fieldWithPath("data.promptType").type(JsonFieldType.STRING).description("프롬프트 유형"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("수정된 프롬프트 내용"),
                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("마지막 수정 일시"),
                        fieldWithPath("data.updatedBy").type(JsonFieldType.STRING).optional().description("마지막 수정자 ID"),
                    ),
                ),
            )
    }
}
