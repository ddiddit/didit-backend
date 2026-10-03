package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminPromptPreview
import com.didit.application.admin.provided.AdminPromptPreviewAnalysisItem
import com.didit.application.admin.provided.AdminPromptPreviewMessage
import com.didit.application.admin.provided.AdminPromptPreviewProgress
import com.didit.application.admin.provided.AdminPromptPreviewResult
import com.didit.application.admin.provided.AdminPromptPreviewState
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationUserIntent
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class AdminPromptPreviewApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminPromptPreview: AdminPromptPreview = mock()

    override fun initController() = AdminPromptPreviewApi(adminPromptPreview)

    @Test
    fun `V2 회고 프롬프트를 미리보기한다`() {
        val userMessageId = UUID.randomUUID()
        val assistantMessage =
            AdminPromptPreviewMessage(
                id = UUID.randomUUID(),
                sender = Sender.AI,
                content = "어떤 방식으로 해결했나요?",
                messageType = ConversationMessageType.CONVERSATION,
            )
        val items =
            RetrospectiveItemType.entries.map {
                AdminPromptPreviewAnalysisItem(
                    itemType = it,
                    status = if (it == RetrospectiveItemType.FACT) RetrospectiveItemStatus.PARTIAL else RetrospectiveItemStatus.EMPTY,
                    summary = if (it == RetrospectiveItemType.FACT) "배포 오류를 해결함" else null,
                )
            }
        whenever(adminPromptPreview.preview(any())).thenReturn(
            AdminPromptPreviewResult(
                assistantMessage = assistantMessage,
                progress = AdminPromptPreviewProgress(filledCount = 1, totalCount = 7),
                nextState = AdminPromptPreviewState(messages = listOf(assistantMessage), analysisItems = items),
            ),
        )
        val request =
            mapOf(
                "job" to "DEVELOPER",
                "experience" to "YEARS_1_TO_2",
                "promptSource" to "DRAFT",
                "draftPrompt" to "저장 전 프롬프트",
                "priorState" to null,
                "userMessageId" to userMessageId,
                "message" to "배포 오류를 해결했어요.",
                "conversationIntent" to "CONTINUE_AFTER_COMPLETION",
            )

        mockMvc
            .perform(
                post("/api/v1/admin/prompts/preview-v2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.progress.filledCount").value(1))
            .andExpect(jsonPath("$.data.nextState.analysisItems[0].status").value("PARTIAL"))
            .andDo(
                document(
                    "admin/prompts/preview-v2",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("job").type(JsonFieldType.STRING).description("직군 (DEVELOPER, PLANNER, DESIGNER)"),
                        fieldWithPath("experience").type(JsonFieldType.STRING).description("경력 구간"),
                        fieldWithPath("promptSource").type(JsonFieldType.STRING).description("프롬프트 출처 (SAVED, DRAFT)"),
                        fieldWithPath("draftPrompt").type(JsonFieldType.STRING).optional().description("DRAFT 선택 시 사용할 저장 전 프롬프트"),
                        fieldWithPath("priorState").type(JsonFieldType.OBJECT).optional().description("직전 응답의 nextState. 첫 턴은 null"),
                        fieldWithPath("userMessageId").type(JsonFieldType.STRING).description("이번 사용자 메시지 ID"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("이번 회고 답변"),
                        fieldWithPath("conversationIntent")
                            .type(JsonFieldType.STRING)
                            .description("대화 의도. 종료 제안 뒤 계속하기는 CONTINUE_AFTER_COMPLETION")
                            .optional(),
                    ),
                    responseFields(
                        fieldWithPath("data.assistantMessage.id").type(JsonFieldType.STRING).description("AI 메시지 ID"),
                        fieldWithPath("data.assistantMessage.sender").type(JsonFieldType.STRING).description("발신자"),
                        fieldWithPath("data.assistantMessage.content").type(JsonFieldType.STRING).description("AI 응답"),
                        fieldWithPath("data.assistantMessage.messageType").type(JsonFieldType.STRING).description("메시지 유형"),
                        fieldWithPath("data.assistantMessage.supportingContent").type(JsonFieldType.NULL).optional().description("보조 문구"),
                        fieldWithPath("data.assistantMessage.relevance").type(JsonFieldType.NULL).optional().description("사용자 메시지 관련성"),
                        fieldWithPath("data.progress.filledCount").type(JsonFieldType.NUMBER).description("채워진 정보 수"),
                        fieldWithPath("data.progress.totalCount").type(JsonFieldType.NUMBER).description("전체 정보 수"),
                        fieldWithPath("data.nextState.messages[].id").type(JsonFieldType.STRING).description("메시지 ID"),
                        fieldWithPath("data.nextState.messages[].sender").type(JsonFieldType.STRING).description("발신자"),
                        fieldWithPath("data.nextState.messages[].content").type(JsonFieldType.STRING).description("메시지 내용"),
                        fieldWithPath("data.nextState.messages[].messageType").type(JsonFieldType.STRING).description("메시지 유형"),
                        fieldWithPath(
                            "data.nextState.messages[].supportingContent",
                        ).type(JsonFieldType.NULL).optional().description("보조 문구"),
                        fieldWithPath("data.nextState.messages[].relevance").type(JsonFieldType.NULL).optional().description("사용자 메시지 관련성"),
                        fieldWithPath("data.nextState.analysisItems[].itemType").type(JsonFieldType.STRING).description("정보 항목"),
                        fieldWithPath("data.nextState.analysisItems[].status").type(JsonFieldType.STRING).description("채움 상태"),
                        fieldWithPath(
                            "data.nextState.analysisItems[].summary",
                        ).type(JsonFieldType.STRING).optional().description("구조화된 요약"),
                        fieldWithPath("data.nextState.analysisItems[].questionAllowed")
                            .type(JsonFieldType.BOOLEAN)
                            .description("해당 항목에 대한 후속 질문 허용 여부"),
                        fieldWithPath("data.nextState.recentQuestionTargets")
                            .type(JsonFieldType.ARRAY)
                            .description("최근 질문한 회고 항목. 다음 요청의 priorState로 그대로 전달"),
                        fieldWithPath("data.nextState.completedQuestionCount")
                            .type(JsonFieldType.NUMBER)
                            .description("완료된 질문 턴 수. 다음 요청의 priorState로 그대로 전달"),
                        fieldWithPath("data.nextState.completionPreviouslyOffered")
                            .type(JsonFieldType.BOOLEAN)
                            .description("회고 종료 제안 이력. 다음 요청의 priorState로 그대로 전달"),
                    ),
                ),
            )

        val command = argumentCaptor<com.didit.application.admin.provided.AdminPromptPreviewCommand>()
        verify(adminPromptPreview).preview(command.capture())
        assertThat(command.firstValue.draftPrompt).isEqualTo("저장 전 프롬프트")
        assertThat(command.firstValue.conversationIntent).isEqualTo(ConversationUserIntent.CONTINUE_AFTER_COMPLETION)
    }
}
