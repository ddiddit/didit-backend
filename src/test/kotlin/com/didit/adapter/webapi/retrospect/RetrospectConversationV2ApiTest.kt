package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.retrospect.dto.SubmitConversationMessageV2Request
import com.didit.application.retrospect.dto.ConversationMessageResult
import com.didit.application.retrospect.dto.ConversationTurnResult
import com.didit.application.retrospect.dto.ConversationV2Result
import com.didit.application.retrospect.dto.FinishConversationV2Result
import com.didit.application.retrospect.dto.StartConversationV2Result
import com.didit.application.retrospect.dto.SubmitConversationMessageResult
import com.didit.application.retrospect.provided.RetrospectiveConversationV2
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationStatus
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.Sender
import com.didit.domain.retrospect.SummaryGenerationStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class RetrospectConversationV2ApiTest : AuthenticatedRestDocsSupport() {
    private val conversation: RetrospectiveConversationV2 = mock(RetrospectiveConversationV2::class.java)
    private val retrospectiveId = UUID.randomUUID()
    private val introMessageId = UUID.randomUUID()
    private val userMessageId = UUID.randomUUID()
    private val assistantMessageId = UUID.randomUUID()
    private val turnId = UUID.randomUUID()
    private val clientMessageId = UUID.randomUUID()
    private val now = LocalDateTime.of(2026, 8, 8, 12, 0)

    override fun initController() = RetrospectConversationV2Api(conversation)

    @Test
    fun `V2 회고 시작`() {
        whenever(conversation.start(userId)).thenReturn(
            StartConversationV2Result(
                retrospectiveId = retrospectiveId,
                conversationStatus = ConversationStatus.ACTIVE,
                initialMessage = introMessage(),
                readyToComplete = false,
            ),
        )

        mockMvc
            .perform(post("/api/v2/retrospectives"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.initialMessage.title").value("오늘 어떤 일을 하셨나요?"))
            .andExpect(jsonPath("$.data.initialMessage.content").isEmpty)
            .andDo(
                document(
                    "retrospect-v2/start",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(*startResponseFields()),
                ),
            )
    }

    @Test
    fun `V2 대화 메시지 전송`() {
        val request = SubmitConversationMessageV2Request(clientMessageId, "배포 자동화 작업을 완료했습니다.")
        whenever(conversation.submitMessage(retrospectiveId, userId, clientMessageId, request.content)).thenReturn(
            SubmitConversationMessageResult(
                turnId = turnId,
                userMessageId = userMessageId,
                assistantMessage = assistantMessage(),
                readyToComplete = false,
            ),
        )

        mockMvc
            .perform(
                post("/api/v2/retrospectives/{retrospectiveId}/messages", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.assistantMessage.content").value("자동화로 가장 크게 줄어든 작업은 무엇인가요?"))
            .andExpect(jsonPath("$.data.assistantMessage.title").isEmpty)
            .andExpect(jsonPath("$.data.assistantMessage.body").isEmpty)
            .andDo(
                document(
                    "retrospect-v2/submit-message",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    requestFields(
                        fieldWithPath("clientMessageId").type(JsonFieldType.STRING).description("중복 전송 방지용 클라이언트 메시지 ID"),
                        fieldWithPath("content").type(JsonFieldType.STRING).description("사용자가 입력한 회고 내용"),
                    ),
                    responseFields(
                        fieldWithPath("data.turnId").type(JsonFieldType.STRING).description("대화 턴 ID"),
                        fieldWithPath("data.userMessageId").type(JsonFieldType.STRING).description("저장된 사용자 메시지 ID"),
                        *messageFields("data.assistantMessage", "AI 후속 대화"),
                        fieldWithPath("data.readyToComplete").type(JsonFieldType.BOOLEAN).description("내부 회고 항목 기준 완료 준비도"),
                    ),
                ),
            )
    }

    @Test
    fun `V2 대화 조회`() {
        whenever(conversation.getConversation(retrospectiveId, userId)).thenReturn(
            ConversationV2Result(
                retrospectiveId = retrospectiveId,
                conversationStatus = ConversationStatus.ACTIVE,
                messages = listOf(introMessage(), assistantMessage()),
                turns =
                    listOf(
                        ConversationTurnResult(
                            id = turnId,
                            clientMessageId = clientMessageId,
                            userMessageId = userMessageId,
                            status = ConversationTurnStatus.COMPLETED,
                            attemptCount = 1,
                            errorCode = null,
                        ),
                    ),
                readyToComplete = false,
            ),
        )

        mockMvc
            .perform(get("/api/v2/retrospectives/{retrospectiveId}/conversation", retrospectiveId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect-v2/get-conversation",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.conversationStatus").type(JsonFieldType.STRING).description("대화 상태"),
                        fieldWithPath("data.messages").type(JsonFieldType.ARRAY).description("전체 대화 메시지"),
                        *messageFields("data.messages[]", "대화 메시지"),
                        fieldWithPath("data.turns").type(JsonFieldType.ARRAY).description("사용자 요청 처리 상태"),
                        fieldWithPath("data.turns[].id").type(JsonFieldType.STRING).description("대화 턴 ID"),
                        fieldWithPath("data.turns[].clientMessageId").type(JsonFieldType.STRING).description("클라이언트 메시지 ID"),
                        fieldWithPath("data.turns[].userMessageId").type(JsonFieldType.STRING).description("사용자 메시지 ID"),
                        fieldWithPath("data.turns[].status").type(JsonFieldType.STRING).description("처리 상태"),
                        fieldWithPath("data.turns[].attemptCount").type(JsonFieldType.NUMBER).description("처리 시도 횟수"),
                        fieldWithPath("data.turns[].errorCode").type(JsonFieldType.STRING).description("실패 코드").optional(),
                        fieldWithPath("data.readyToComplete").type(JsonFieldType.BOOLEAN).description("내부 회고 항목 기준 완료 준비도"),
                    ),
                ),
            )
    }

    @Test
    fun `V2 대화 종료`() {
        whenever(conversation.finish(retrospectiveId, userId)).thenReturn(
            FinishConversationV2Result(
                retrospectiveId = retrospectiveId,
                conversationStatus = ConversationStatus.FINISHED,
                resultGenerationStatus = SummaryGenerationStatus.NOT_STARTED,
            ),
        )

        mockMvc
            .perform(post("/api/v2/retrospectives/{retrospectiveId}/finish", retrospectiveId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect-v2/finish",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.conversationStatus").type(JsonFieldType.STRING).description("종료된 대화 상태"),
                        fieldWithPath("data.resultGenerationStatus")
                            .type(JsonFieldType.STRING)
                            .description("별도 결과 생성 상태. 대화 종료 시에는 NOT_STARTED"),
                    ),
                ),
            )
    }

    private fun introMessage() =
        ConversationMessageResult(
            id = introMessageId,
            sender = Sender.AI,
            messageType = ConversationMessageType.INTRO,
            title = "오늘 어떤 일을 하셨나요?",
            body = "오늘 진행한 일 중 하나를 떠올려, 작업 내용과 함께 결과나 상태도 같이 적어보세요.",
            createdAt = now,
        )

    private fun assistantMessage() =
        ConversationMessageResult(
            id = assistantMessageId,
            sender = Sender.AI,
            messageType = ConversationMessageType.CONVERSATION,
            content = "자동화로 가장 크게 줄어든 작업은 무엇인가요?",
            createdAt = now,
        )

    private fun startResponseFields() =
        arrayOf(
            fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
            fieldWithPath("data.conversationStatus").type(JsonFieldType.STRING).description("대화 상태"),
            *messageFields("data.initialMessage", "첫 안내 메시지"),
            fieldWithPath("data.readyToComplete").type(JsonFieldType.BOOLEAN).description("내부 회고 항목 기준 완료 준비도"),
        )

    private fun messageFields(
        path: String,
        description: String,
    ) = arrayOf(
        fieldWithPath("$path.id").type(JsonFieldType.STRING).description("$description ID"),
        fieldWithPath("$path.sender").type(JsonFieldType.STRING).description("발신자"),
        fieldWithPath("$path.messageType").type(JsonFieldType.STRING).description("메시지 유형"),
        fieldWithPath("$path.title").type(JsonFieldType.STRING).description("INTRO 제목").optional(),
        fieldWithPath("$path.body").type(JsonFieldType.STRING).description("INTRO 보조 문구").optional(),
        fieldWithPath("$path.content").type(JsonFieldType.STRING).description("INTRO 이후 대화 내용").optional(),
        fieldWithPath("$path.createdAt").type(JsonFieldType.STRING).description("생성 시각").optional(),
    )
}
