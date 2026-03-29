package com.didit.adapter.webapi.retrospect

import com.didit.application.retrospect.RetrospectQueryService
import com.didit.application.retrospect.dto.ChatMessageDto
import com.didit.application.retrospect.dto.CompleteRetrospectiveResponse
import com.didit.application.retrospect.dto.GetRetrospectiveResponse
import com.didit.application.retrospect.dto.StartRetrospectiveResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.provided.RetrospectService
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class RetrospectApiTest : AuthenticatedRestDocsSupport() {
    private val retrospectService: RetrospectService = mock(RetrospectService::class.java)
    private val retrospectQueryService: RetrospectQueryService = mock(RetrospectQueryService::class.java)

    override fun initController() = RetrospectApi(retrospectService, retrospectQueryService)

    @Test
    fun `회고 시작`() {
        val response =
            StartRetrospectiveResponse(
                retrospectiveId = userId.toString(),
                questionNumber = 1,
                question = "오늘 가장 잘한 일은 무엇인가요?",
                isDeepQuestion = false,
            )
        whenever(retrospectService.startRetrospective(userId)).thenReturn(response)

        mockMvc
            .perform(post("/api/v1/retrospectives/start"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/start",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.questionNumber").type(JsonFieldType.NUMBER).description("현재 질문 번호"),
                        fieldWithPath("data.question").type(JsonFieldType.STRING).description("질문 내용"),
                        fieldWithPath("data.isDeepQuestion").type(JsonFieldType.BOOLEAN).description("심화 질문 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `답변 제출`() {
        val request = SubmitAnswerRequest(answer = "열심히 했습니다.")
        val response =
            SubmitAnswerResponse(
                retrospectiveId = userId.toString(),
                questionNumber = 2,
                nextQuestion = "그 이유가 무엇인가요?",
                isDeepQuestion = false,
                isCompleted = false,
                summary = null,
            )
        whenever(retrospectService.submitAnswer(any(), any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/retrospectives/answer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/answer",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("answer").type(JsonFieldType.STRING).description("사용자 답변"),
                    ),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.questionNumber").type(JsonFieldType.NUMBER).description("다음 질문 번호"),
                        fieldWithPath("data.nextQuestion").type(JsonFieldType.STRING).optional().description("다음 질문 (완료 시 null)"),
                        fieldWithPath("data.isDeepQuestion").type(JsonFieldType.BOOLEAN).description("심화 질문 여부"),
                        fieldWithPath("data.isCompleted").type(JsonFieldType.BOOLEAN).description("회고 완료 여부"),
                        fieldWithPath("data.summary").type(JsonFieldType.STRING).optional().description("회고 요약 (완료 시 반환)"),
                    ),
                ),
            )
    }

    @Test
    fun `회고 조회`() {
        val response =
            GetRetrospectiveResponse(
                retrospectiveId = userId.toString(),
                currentQuestionNumber = 2,
                isCompleted = false,
                chatHistory =
                    listOf(
                        ChatMessageDto(
                            questionNumber = 1,
                            content = "오늘 가장 잘한 일은 무엇인가요?",
                            isAnswer = false,
                            isDeepQuestion = false,
                            createdAt = "2024-01-01T00:00:00",
                        ),
                    ),
                summary = null,
            )
        whenever(retrospectQueryService.getRetrospective(userId)).thenReturn(response)

        mockMvc
            .perform(get("/api/v1/retrospectives"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/get",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.currentQuestionNumber").type(JsonFieldType.NUMBER).description("현재 질문 번호"),
                        fieldWithPath("data.isCompleted").type(JsonFieldType.BOOLEAN).description("회고 완료 여부"),
                        fieldWithPath("data.chatHistory[].questionNumber").type(JsonFieldType.NUMBER).description("질문 번호"),
                        fieldWithPath("data.chatHistory[].content").type(JsonFieldType.STRING).description("메시지 내용"),
                        fieldWithPath("data.chatHistory[].isAnswer").type(JsonFieldType.BOOLEAN).description("답변 여부"),
                        fieldWithPath("data.chatHistory[].isDeepQuestion").type(JsonFieldType.BOOLEAN).description("심화 질문 여부"),
                        fieldWithPath("data.chatHistory[].createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                        fieldWithPath("data.summary").type(JsonFieldType.STRING).optional().description("회고 요약"),
                    ),
                ),
            )
    }

    @Test
    fun `회고 완료 조회`() {
        val response =
            CompleteRetrospectiveResponse(
                retrospectiveId = userId.toString(),
                summary = "오늘 회고를 통해 많은 것을 배웠습니다.",
            )
        whenever(retrospectQueryService.completeRetrospective(userId)).thenReturn(response)

        mockMvc
            .perform(get("/api/v1/retrospectives/complete"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/complete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.summary").type(JsonFieldType.STRING).description("회고 요약"),
                    ),
                ),
            )
    }
}
