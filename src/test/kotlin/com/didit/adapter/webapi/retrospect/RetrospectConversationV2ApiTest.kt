package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.retrospect.dto.SubmitConversationMessageV2Request
import com.didit.application.retrospect.dto.ConversationMessageResult
import com.didit.application.retrospect.dto.ConversationTurnResult
import com.didit.application.retrospect.dto.ConversationV2Result
import com.didit.application.retrospect.dto.FinishConversationV2Result
import com.didit.application.retrospect.dto.RetrospectiveResultV2Result
import com.didit.application.retrospect.dto.StartConversationV2Result
import com.didit.application.retrospect.dto.SubmitConversationMessageResult
import com.didit.application.retrospect.provided.RetrospectiveConversationV2
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationStatus
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.Sender
import com.didit.domain.retrospect.SummaryGenerationStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
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
        whenever(conversation.submitMessage(retrospectiveId, userId, clientMessageId, request.content, request.inputType)).thenReturn(
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
                        fieldWithPath("inputType").type(JsonFieldType.STRING).description("입력 출처. 생략 시 TEXT").optional(),
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
    fun `V2 메시지 입력 타입을 생략하면 TEXT로 전달한다`() {
        whenever(conversation.submitMessage(retrospectiveId, userId, clientMessageId, "직접 입력했습니다.", InputType.TEXT))
            .thenReturn(submitMessageResult())

        mockMvc
            .perform(
                post("/api/v2/retrospectives/{retrospectiveId}/messages", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"clientMessageId":"$clientMessageId","content":"직접 입력했습니다."}""",
                    ),
            ).andExpect(status().isOk)

        verify(conversation).submitMessage(retrospectiveId, userId, clientMessageId, "직접 입력했습니다.", InputType.TEXT)
    }

    @Test
    fun `V2 STT 메시지는 입력 타입을 STT로 전달한다`() {
        val request = SubmitConversationMessageV2Request(clientMessageId, "음성 결과를 수정했습니다.", InputType.STT)
        whenever(conversation.submitMessage(retrospectiveId, userId, clientMessageId, request.content, InputType.STT))
            .thenReturn(submitMessageResult())

        mockMvc
            .perform(
                post("/api/v2/retrospectives/{retrospectiveId}/messages", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)

        verify(conversation).submitMessage(retrospectiveId, userId, clientMessageId, request.content, InputType.STT)
    }

    @Test
    fun `V2 메시지에 지원하지 않는 입력 타입을 전달하면 400을 반환한다`() {
        mockMvc
            .perform(
                post("/api/v2/retrospectives/{retrospectiveId}/messages", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"clientMessageId":"$clientMessageId","content":"회고 내용","inputType":"VOICE"}""",
                    ),
            ).andExpect(status().isBadRequest)
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
                resultGenerationStatus = SummaryGenerationStatus.GENERATED,
                title = "배포 오류 롤백 회고",
                result =
                    RetrospectiveResultV2Result(
                        summary = "배포 오류를 발견하고 롤백했다.",
                        strength = null,
                        improvement = "배포 전 확인이 부족했다.",
                        process = "로그를 확인해 원인을 좁혔다.",
                        learning = "배포 체크리스트가 필요하다.",
                        insight = null,
                        nextActions = listOf("배포 체크리스트를 만든다."),
                    ),
            ),
        )

        mockMvc
            .perform(post("/api/v2/retrospectives/{retrospectiveId}/finish", retrospectiveId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.result.strength").value("오늘 잘한 점은 대화에서 확인되지 않았어요."))
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
                            .description("구조화 결과 생성 상태. 성공 시 GENERATED"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("자동 생성된 구체적인 회고 제목"),
                        fieldWithPath("data.result.summary").type(JsonFieldType.STRING).description("회고 요약").optional(),
                        fieldWithPath("data.result.strength").type(JsonFieldType.STRING).description("오늘 잘한 점").optional(),
                        fieldWithPath("data.result.improvement").type(JsonFieldType.STRING).description("아쉬웠던 지점").optional(),
                        fieldWithPath("data.result.process").type(JsonFieldType.STRING).description("돌아본 과정").optional(),
                        fieldWithPath("data.result.learning").type(JsonFieldType.STRING).description("오늘의 배움").optional(),
                        fieldWithPath("data.result.insight").type(JsonFieldType.STRING).description("디딧의 인사이트").optional(),
                        fieldWithPath("data.result.nextActions").type(JsonFieldType.ARRAY).description("다음에 해볼 일, 최대 3개").optional(),
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

    private fun submitMessageResult() =
        SubmitConversationMessageResult(
            turnId = turnId,
            userMessageId = userMessageId,
            assistantMessage = assistantMessage(),
            readyToComplete = false,
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
