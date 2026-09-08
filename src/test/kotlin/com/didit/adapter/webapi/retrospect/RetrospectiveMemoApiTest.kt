package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.retrospect.dto.RetrospectiveMemoContentRequest
import com.didit.application.retrospect.dto.RetrospectiveMemoResult
import com.didit.application.retrospect.provided.RetrospectiveMemoFinder
import com.didit.application.retrospect.provided.RetrospectiveMemoModifier
import com.didit.application.retrospect.provided.RetrospectiveMemoRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RetrospectiveMemoApiTest : AuthenticatedRestDocsSupport() {
    private val memoRegister: RetrospectiveMemoRegister = mock(RetrospectiveMemoRegister::class.java)
    private val memoFinder: RetrospectiveMemoFinder = mock(RetrospectiveMemoFinder::class.java)
    private val memoModifier: RetrospectiveMemoModifier = mock(RetrospectiveMemoModifier::class.java)
    private val retrospectiveId = UUID.randomUUID()
    private val memoId = UUID.randomUUID()
    private val memo =
        RetrospectiveMemoResult(
            id = memoId,
            content = "다음 배포 전에 예외 케이스를 점검한다.",
            memoDate = LocalDate.of(2026, 9, 8),
            createdAt = LocalDateTime.of(2026, 9, 8, 10, 0),
            updatedAt = LocalDateTime.of(2026, 9, 8, 10, 0),
        )

    override fun initController() = RetrospectiveMemoApi(memoRegister, memoFinder, memoModifier)

    @Test
    fun `회고 메모 작성`() {
        val request = RetrospectiveMemoContentRequest(content = memo.content)
        whenever(memoRegister.create(retrospectiveId, userId, request.content)).thenReturn(memo)

        mockMvc
            .perform(
                post("/api/v2/retrospectives/{retrospectiveId}/memos", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(memoId.toString()))
            .andDo(
                document(
                    "retrospect-v2/memo/create",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("메모 내용")),
                    responseFields(*memoResponseFields("data")),
                ),
            )
    }

    @Test
    fun `회고 메모 목록 조회`() {
        whenever(memoFinder.findAll(retrospectiveId, userId)).thenReturn(listOf(memo))

        mockMvc
            .perform(get("/api/v2/retrospectives/{retrospectiveId}/memos", retrospectiveId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].content").value(memo.content))
            .andDo(
                document(
                    "retrospect-v2/memo/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    responseFields(*memoResponseFields("data[]")),
                ),
            )
    }

    @Test
    fun `회고 메모 수정`() {
        val request = RetrospectiveMemoContentRequest(content = "수정한 메모")
        whenever(memoModifier.update(retrospectiveId, memoId, userId, request.content)).thenReturn(memo.copy(content = request.content))

        mockMvc
            .perform(
                patch("/api/v2/retrospectives/{retrospectiveId}/memos/{memoId}", retrospectiveId, memoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").value(request.content))
            .andDo(
                document(
                    "retrospect-v2/memo/update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                        parameterWithName("memoId").description("수정할 메모 ID"),
                    ),
                    requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("수정할 메모 내용")),
                    responseFields(*memoResponseFields("data")),
                ),
            )
    }

    @Test
    fun `회고 메모 삭제`() {
        doNothing().whenever(memoModifier).delete(retrospectiveId, memoId, userId)

        mockMvc
            .perform(delete("/api/v2/retrospectives/{retrospectiveId}/memos/{memoId}", retrospectiveId, memoId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "retrospect-v2/memo/delete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                        parameterWithName("memoId").description("삭제할 메모 ID"),
                    ),
                ),
            )
    }

    private fun memoResponseFields(path: String) =
        arrayOf(
            fieldWithPath("$path.id").type(JsonFieldType.STRING).description("메모 ID"),
            fieldWithPath("$path.content").type(JsonFieldType.STRING).description("메모 내용"),
            fieldWithPath("$path.memoDate").type(JsonFieldType.STRING).description("메모 작성 기준일"),
            fieldWithPath("$path.createdAt").type(JsonFieldType.STRING).description("생성 시각").optional(),
            fieldWithPath("$path.updatedAt").type(JsonFieldType.STRING).description("수정 시각").optional(),
        )
}
