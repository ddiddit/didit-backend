package com.didit.adapter.webapi.retrospect

import com.didit.application.retrospect.RetrospectiveFeedbackService
import com.didit.application.retrospect.required.RetrospectiveFeedbackRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.retrospect.FeedbackRating
import com.didit.domain.retrospect.FeedbackReason
import com.didit.domain.retrospect.RetrospectiveFeedback
import com.didit.support.RetrospectiveFixture
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class RetrospectiveFeedbackApiTest : AuthenticatedRestDocsSupport() {
    private val retrospectives = mock<RetrospectiveRepository>()
    private val feedbacks = mock<RetrospectiveFeedbackRepository>()
    private val service = RetrospectiveFeedbackService(retrospectives, feedbacks)
    private val retrospective = RetrospectiveFixture.createCompleted(userId)
    private val id = retrospective.id

    override fun initController() = RetrospectiveFeedbackApi(service, service)

    @Test
    fun `피드백 등록 변경 API와 문서`() {
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNullForUpdate(id, userId)).thenReturn(retrospective)
        whenever(feedbacks.save(any())).thenAnswer { it.arguments[0] }
        mockMvc
            .perform(
                put("/api/v2/retrospectives/{retrospectiveId}/feedback", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"rating":"HELPFUL","reasons":["CORE_IDENTIFIED","CUSTOM"],"comment":"핵심 정리가 좋았어요"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.rating").value("HELPFUL"))
            .andExpect(jsonPath("$.data.comment").value("핵심 정리가 좋았어요"))
            .andDo(
                document(
                    "retrospect-v2/feedback/submit",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    requestFields(
                        fieldWithPath("rating").description("HELPFUL 또는 NEEDS_IMPROVEMENT"),
                        fieldWithPath("reasons").description("선택 사유 코드. CUSTOM 포함 최대 3개").optional(),
                        fieldWithPath("comment").description("CUSTOM 선택 시 필수. 최대 500자").optional(),
                    ),
                    responseFields(*feedbackResponseFields()),
                ),
            )
    }

    @Test
    fun `제출한 피드백을 조회한다`() {
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNull(id, userId)).thenReturn(retrospective)
        whenever(feedbacks.findByRetrospectiveId(id)).thenReturn(
            RetrospectiveFeedback.create(id, FeedbackRating.NEEDS_IMPROVEMENT, listOf(FeedbackReason.MISSED_CONTEXT), null),
        )
        mockMvc
            .perform(get("/api/v2/retrospectives/{retrospectiveId}/feedback", id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reasons[0]").value("MISSED_CONTEXT"))
            .andDo(
                document(
                    "retrospect-v2/feedback/find",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    responseFields(*feedbackResponseFields()),
                ),
            )
    }

    @Test
    fun `미제출 피드백은 null로 조회한다`() {
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNull(id, userId)).thenReturn(retrospective)
        mockMvc
            .perform(get("/api/v2/retrospectives/{retrospectiveId}/feedback", id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `필수 평가 누락과 알 수 없는 코드 및 501자 입력은 400이다`() {
        val bodies =
            listOf(
                """{}""",
                """{"rating":0}""",
                """{"rating":"UNKNOWN"}""",
                """{"rating":"HELPFUL","reasons":["UNKNOWN"]}""",
                """{"rating":"HELPFUL","reasons":[0]}""",
                objectMapper.writeValueAsString(mapOf("rating" to "HELPFUL", "reasons" to listOf("CUSTOM"), "comment" to "가".repeat(501))),
            )
        bodies.forEach { body ->
            mockMvc
                .perform(
                    put("/api/v2/retrospectives/{retrospectiveId}/feedback", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `평가 불일치 사유와 공백 직접 입력 및 네 사유는 400이다`() {
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNullForUpdate(id, userId)).thenReturn(retrospective)
        val bodies =
            listOf(
                """{"rating":"HELPFUL","reasons":["MISSED_CONTEXT"]}""",
                """{"rating":"HELPFUL","reasons":[null]}""",
                """{"rating":"HELPFUL","reasons":["CUSTOM"],"comment":"   "}""",
                """{"rating":"HELPFUL","reasons":["CORE_IDENTIFIED","DEEP_QUESTIONS","WELL_ORGANIZED","CUSTOM"],"comment":"의견"}""",
            )
        bodies.forEach { body ->
            mockMvc
                .perform(
                    put("/api/v2/retrospectives/{retrospectiveId}/feedback", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `타인 또는 삭제된 회고 요청은 404이다`() {
        mockMvc.perform(get("/api/v2/retrospectives/{retrospectiveId}/feedback", id)).andExpect(status().isNotFound)
        mockMvc
            .perform(
                put("/api/v2/retrospectives/{retrospectiveId}/feedback", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"rating":"HELPFUL"}"""),
            ).andExpect(status().isNotFound)
    }

    private fun feedbackResponseFields() =
        arrayOf(
            fieldWithPath("data.id").type(JsonFieldType.STRING).description("피드백 ID"),
            fieldWithPath("data.rating").type(JsonFieldType.STRING).description("평가 코드"),
            fieldWithPath("data.reasons").type(JsonFieldType.ARRAY).description("선택 사유 코드"),
            fieldWithPath("data.comment").type(JsonFieldType.STRING).description("직접 입력 의견").optional(),
            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("최초 생성 시각").optional(),
            fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("최종 변경 시각").optional(),
        )
}
