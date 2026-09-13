package com.didit.application.admin

import com.didit.application.admin.provided.AdminPromptPreviewAnalysisItem
import com.didit.application.admin.provided.AdminPromptPreviewCommand
import com.didit.application.admin.provided.AdminPromptPreviewMessage
import com.didit.application.admin.provided.AdminPromptPreviewState
import com.didit.application.admin.provided.AdminPromptSource
import com.didit.application.admin.required.AdminPromptPreviewAIClient
import com.didit.application.common.exception.BusinessException
import com.didit.application.prompt.required.PromptRepository
import com.didit.application.retrospect.ConversationV2TurnPolicy
import com.didit.application.retrospect.required.ConversationAnalysisUpdate
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.auth.UserExperience
import com.didit.domain.prompt.Prompt
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import com.didit.domain.shared.Job
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.UUID

class AdminPromptPreviewServiceTest {
    private val promptRepository = mock<PromptRepository>()
    private val aiClient = mock<AdminPromptPreviewAIClient>()
    private val service = AdminPromptPreviewService(promptRepository, aiClient, ConversationV2TurnPolicy())

    @BeforeEach
    fun setUp() {
        whenever(aiClient.preview(any(), any())).thenReturn(generatedResult())
    }

    @Test
    fun `저장된 V2 프롬프트로 첫 preview를 실행한다`() {
        whenever(promptRepository.findByJobTypeAndPromptType(PromptJobType.DEVELOPER, PromptType.CONVERSATION_V2))
            .thenReturn(Prompt(jobType = PromptJobType.DEVELOPER, promptType = PromptType.CONVERSATION_V2, content = "저장 템플릿"))
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()

        val result = service.preview(command(promptSource = AdminPromptSource.SAVED))

        verify(aiClient).preview(org.mockito.kotlin.eq("저장 템플릿"), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.job).isEqualTo(Job.DEVELOPER)
        assertThat(result.assistantMessage.content).isEqualTo("확인했어요.\n다음은 무엇인가요?")
        verify(promptRepository, never()).save(any())
    }

    @Test
    fun `초안은 저장하지 않고 전달된 템플릿으로 실행한다`() {
        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "저장 전 초안"))

        verify(aiClient).preview(org.mockito.kotlin.eq("저장 전 초안"), any())
        assertThat(result.nextState.messages).anyMatch { it.content == "첫 업무 회고" }
        verify(promptRepository, never()).findByJobTypeAndPromptType(any(), any())
        verify(promptRepository, never()).save(any())
    }

    @Test
    fun `첫 턴은 intro와 일곱 EMPTY 분석 항목에서 시작한다`() {
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()

        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))

        verify(aiClient).preview(any(), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.messages).hasSize(2)
        assertThat(
            requestCaptor.firstValue.messages
                .first()
                .content,
        ).isEqualTo("오늘 어떤 일을 하셨나요?")
        assertThat(requestCaptor.firstValue.analysisItems)
            .extracting<com.didit.domain.retrospect.RetrospectiveItemStatus> { it.status }
            .containsOnly(RetrospectiveItemStatus.EMPTY)
        assertThat(
            result.nextState.messages
                .first()
                .messageType,
        ).isEqualTo(ConversationMessageType.INTRO)
        assertThat(result.nextState.analysisItems).hasSize(7)
    }

    @Test
    fun `후속 턴은 이전 상태를 보존하고 관련 분석 항목을 갱신한다`() {
        val first = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                relevance = MessageRelevance.RETROSPECTIVE,
                updates = listOf(update(RetrospectiveItemType.PROCESS, request.currentMessageId, RetrospectiveItemStatus.ENOUGH)),
            )
        }

        val second = service.preview(command(priorState = first.nextState, message = "후속 업무 회고"))

        assertThat(second.nextState.messages)
            .extracting<String> { it.content }
            .containsSubsequence("오늘 어떤 일을 하셨나요?", "첫 업무 회고", "확인했어요.\n다음은 무엇인가요?", "후속 업무 회고")
        assertThat(
            second.nextState.analysisItems
                .single { it.itemType == RetrospectiveItemType.PROCESS }
                .status,
        ).isEqualTo(RetrospectiveItemStatus.ENOUGH)
    }

    @Test
    fun `채워진 분석 항목 수를 진행도로 반환한다`() {
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                updates = listOf(update(RetrospectiveItemType.FACT, request.currentMessageId, RetrospectiveItemStatus.PARTIAL)),
            )
        }

        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))

        assertThat(result.progress.filledCount).isEqualTo(1)
        assertThat(result.progress.totalCount).isEqualTo(7)
    }

    @Test
    fun `비회고 응답의 분석 갱신은 무시한다`() {
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                relevance = MessageRelevance.OFF_TOPIC,
                updates = listOf(update(RetrospectiveItemType.FACT, request.currentMessageId, RetrospectiveItemStatus.ENOUGH)),
            )
        }

        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))

        assertThat(result.nextState.analysisItems).allMatch { it.status == RetrospectiveItemStatus.EMPTY }
    }

    @Test
    fun `내용이 없는 AI 응답은 상태에 추가하지 않고 실패한다`() {
        whenever(aiClient.preview(any(), any())).thenReturn(
            generatedTurn().copy(acknowledgement = " ", interpretation = "", question = ""),
        )

        assertThatThrownBy { service.preview(command()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("AI 응답이 비어 있습니다.")
    }

    @Test
    fun `운영 설정의 context 문자 제한을 preview에도 적용한다`() {
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()
        val oldMessage = message(UUID.randomUUID()).copy(content = "12345678")
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(oldMessage),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
            )

        ApplicationContextRunner()
            .withPropertyValues("retrospective.v2.max-context-characters=10")
            .withBean(PromptRepository::class.java, { promptRepository })
            .withBean(AdminPromptPreviewAIClient::class.java, { aiClient })
            .withBean(ConversationV2TurnPolicy::class.java)
            .withBean(AdminPromptPreviewService::class.java)
            .run { context ->
                context.getBean(AdminPromptPreviewService::class.java).preview(
                    command(priorState = priorState, message = "1234"),
                )
            }

        verify(aiClient).preview(any(), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.messages)
            .extracting<String> { it.content }
            .containsExactly("1234")
    }

    @Test
    fun `잘못된 초안 메시지 또는 상태는 AI 호출 전에 거절한다`() {
        val duplicateId = UUID.randomUUID()
        val invalidState =
            AdminPromptPreviewState(
                messages = listOf(message(id = duplicateId), message(id = duplicateId)),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(
                            it,
                            RetrospectiveItemStatus.EMPTY,
                            null,
                        )
                    },
            )

        assertThatThrownBy { service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = " ")) }
            .isInstanceOf(BusinessException::class.java)
        assertThatThrownBy { service.preview(command(message = "  ")) }
            .isInstanceOf(BusinessException::class.java)
        assertThatThrownBy { service.preview(command(priorState = invalidState)) }
            .isInstanceOf(BusinessException::class.java)
        verify(aiClient, never()).preview(any(), any())
    }

    private fun command(
        promptSource: AdminPromptSource = AdminPromptSource.DRAFT,
        draftPrompt: String? = "초안",
        priorState: AdminPromptPreviewState? = null,
        message: String = "첫 업무 회고",
    ) = AdminPromptPreviewCommand(
        job = Job.DEVELOPER,
        experience = UserExperience.YEARS_1_TO_2,
        promptSource = promptSource,
        draftPrompt = draftPrompt,
        priorState = priorState,
        userMessageId = UUID.randomUUID(),
        message = message,
    )

    private fun message(id: UUID) =
        AdminPromptPreviewMessage(
            id = id,
            sender = Sender.USER,
            content = "기존 메시지",
            messageType = ConversationMessageType.CONVERSATION,
            relevance = MessageRelevance.RETROSPECTIVE,
        )

    private fun generatedResult() = generatedTurn()

    private fun generatedTurn(
        relevance: MessageRelevance = MessageRelevance.RETROSPECTIVE,
        updates: List<ConversationAnalysisUpdate> = emptyList(),
    ) = GeneratedConversationTurn(
        acknowledgement = "확인했어요.",
        interpretation = "",
        question = "다음은 무엇인가요?",
        questionTarget = RetrospectiveItemType.FACT,
        relevance = relevance,
        analysisUpdates = updates,
        inputTokens = 10,
        outputTokens = 5,
    )

    private fun update(
        itemType: RetrospectiveItemType,
        messageId: UUID,
        status: RetrospectiveItemStatus,
    ) = ConversationAnalysisUpdate(itemType, status, "$itemType 요약", listOf(messageId))
}
