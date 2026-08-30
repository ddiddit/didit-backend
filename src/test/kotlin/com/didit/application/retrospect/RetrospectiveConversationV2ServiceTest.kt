package com.didit.application.retrospect

import com.didit.adapter.config.JpaAuditingConfig
import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.exception.ConversationAiFailedException
import com.didit.application.retrospect.exception.DuplicateMessageContentMismatchException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.exception.RetrospectiveResultGenerationFailedException
import com.didit.application.retrospect.exception.SummaryGenerationInProgressException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.required.ChatMessageRepository
import com.didit.application.retrospect.required.ConversationAnalysisUpdate
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.application.retrospect.required.ConversationV2AIClient
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.application.retrospect.required.GeneratedRetrospectiveResultV2
import com.didit.application.retrospect.required.RetrospectiveAnalysisItemRepository
import com.didit.application.retrospect.required.RetrospectivePolicy
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.RetrospectiveResultV2AIClient
import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import com.didit.domain.auth.UserRegisterRequest
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    RetrospectiveConversationV2Service::class,
    RetrospectiveResultV2CompletionCoordinator::class,
    RetrospectiveConversationV2ServiceTest.MetricsConfig::class,
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RetrospectiveConversationV2ServiceTest {
    @Autowired
    private lateinit var service: RetrospectiveConversationV2Service

    @Autowired
    private lateinit var retrospectiveRepository: RetrospectiveRepository

    @Autowired
    private lateinit var analysisItemRepository: RetrospectiveAnalysisItemRepository

    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository

    @MockitoBean
    private lateinit var retrospectiveFinder: RetrospectiveFinder

    @MockitoBean
    private lateinit var retrospectivePolicy: RetrospectivePolicy

    @MockitoBean
    private lateinit var userFinder: UserFinder

    @MockitoBean
    private lateinit var aiClient: ConversationV2AIClient

    @MockitoBean
    private lateinit var resultAIClient: RetrospectiveResultV2AIClient

    @MockitoBean
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val userId = UUID.randomUUID()
    private val user =
        User.register(
            UserRegisterRequest(
                provider = Provider.GOOGLE,
                providerId = "v2-test-user",
                email = "v2-test@didit.test",
            ),
        )

    @BeforeEach
    fun setUp() {
        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(retrospectiveFinder.countByUserIdAndDate(any(), any())).thenReturn(0)
        whenever(retrospectivePolicy.isWhitelisted(any())).thenReturn(true)
    }

    @Test
    fun `대화를 시작하면 INTRO와 일곱 개 분석 항목을 저장한다`() {
        val result = service.start(userId)

        assertThat(result.initialMessage.messageType).isEqualTo(ConversationMessageType.INTRO)
        assertThat(result.initialMessage.title).isEqualTo("오늘 어떤 일을 하셨나요?")
        assertThat(result.initialMessage.body)
            .isEqualTo("오늘 진행한 일 중 하나를 떠올려, 작업 내용과 함께 결과나 상태도 같이 적어보세요.")
        assertThat(analysisItemRepository.findAllByRetrospectiveIdOrderByItemTypeAsc(result.retrospectiveId)).hasSize(7)
    }

    @Test
    fun `완료된 clientMessageId를 재전송하면 AI를 다시 호출하지 않는다`() {
        val started = service.start(userId)
        val clientMessageId = UUID.randomUUID()
        whenever(aiClient.generateConversationTurn(any())).thenAnswer { invocation ->
            retrospectiveResponse(invocation.getArgument(0))
        }

        val first = service.submitMessage(started.retrospectiveId, userId, clientMessageId, "배포 자동화를 완료했습니다.")
        val duplicate = service.submitMessage(started.retrospectiveId, userId, clientMessageId, "배포 자동화를 완료했습니다.")

        assertThat(duplicate.turnId).isEqualTo(first.turnId)
        assertThat(duplicate.assistantMessage.id).isEqualTo(first.assistantMessage.id)
        assertThat(duplicate.readyToComplete).isTrue()
        verify(aiClient, times(1)).generateConversationTurn(any())
    }

    @Test
    fun `AI 실패 턴은 같은 메시지 ID로 재시도할 수 있다`() {
        val started = service.start(userId)
        val clientMessageId = UUID.randomUUID()
        whenever(aiClient.generateConversationTurn(any()))
            .thenThrow(IllegalStateException("temporary failure"))
            .thenAnswer { invocation -> retrospectiveResponse(invocation.getArgument(0)) }

        assertThrows<ConversationAiFailedException> {
            service.submitMessage(started.retrospectiveId, userId, clientMessageId, "장애 원인을 찾았습니다.")
        }

        val retried = service.submitMessage(started.retrospectiveId, userId, clientMessageId, "장애 원인을 찾았습니다.")
        val restored = service.getConversation(started.retrospectiveId, userId)

        assertThat(retried.assistantMessage.content).contains("가장 효과가 있었나요")
        assertThat(restored.turns.single().status).isEqualTo(ConversationTurnStatus.COMPLETED)
        assertThat(restored.turns.single().attemptCount).isEqualTo(2)
        verify(aiClient, times(2)).generateConversationTurn(any())
    }

    @Test
    fun `STT 입력은 수정된 내용과 입력 타입을 저장하고 동일한 대화 흐름을 진행한다`() {
        val started = service.start(userId)
        whenever(aiClient.generateConversationTurn(any())).thenAnswer { invocation ->
            retrospectiveResponse(invocation.getArgument(0))
        }

        service.submitMessage(started.retrospectiveId, userId, UUID.randomUUID(), "수정한 음성 회고입니다.", InputType.STT)

        val userMessage =
            chatMessageRepository
                .findAllByRetrospectiveIdOrderByCreatedAtAsc(started.retrospectiveId)
                .single { it.sender.name == "USER" }
        assertThat(userMessage.content).isEqualTo("수정한 음성 회고입니다.")
        assertThat(userMessage.inputType).isEqualTo(InputType.STT)
        verify(aiClient).generateConversationTurn(any())
    }

    @Test
    fun `같은 메시지 ID의 입력 타입이 달라지면 중복 요청을 거절한다`() {
        val started = service.start(userId)
        val clientMessageId = UUID.randomUUID()
        whenever(aiClient.generateConversationTurn(any())).thenAnswer { invocation ->
            retrospectiveResponse(invocation.getArgument(0))
        }
        service.submitMessage(started.retrospectiveId, userId, clientMessageId, "같은 내용", InputType.TEXT)

        assertThrows<DuplicateMessageContentMismatchException> {
            service.submitMessage(started.retrospectiveId, userId, clientMessageId, "같은 내용", InputType.STT)
        }

        verify(aiClient, times(1)).generateConversationTurn(any())
    }

    @Test
    fun `종료는 관련 메시지만으로 구조화 결과를 생성하고 저장한다`() {
        val started = service.start(userId)
        whenever(aiClient.generateConversationTurn(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<ConversationTurnAIRequest>(0)
            if (request.messages.last().content == "자바 정렬 코드를 짜줘.") offTopicResponse() else retrospectiveResponse(request)
        }
        service.submitMessage(started.retrospectiveId, userId, UUID.randomUUID(), "자바 정렬 코드를 짜줘.")
        service.submitMessage(started.retrospectiveId, userId, UUID.randomUUID(), "배포 오류를 찾아 롤백했습니다.")
        whenever(resultAIClient.generateResult(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.RetrospectiveResultV2AIRequest>(0)
            assertThat(request.messages).hasSize(1)
            assertThat(request.messages.single().content).isEqualTo("배포 오류를 찾아 롤백했습니다.")
            generatedResult()
        }

        val result = service.finish(started.retrospectiveId, userId)
        val saved = checkNotNull(retrospectiveRepository.findByIdAndUserId(started.retrospectiveId, userId))

        assertThat(result.conversationStatus.name).isEqualTo("FINISHED")
        assertThat(result.resultGenerationStatus.name).isEqualTo("GENERATED")
        assertThat(result.title).isEqualTo("배포 오류 롤백 회고")
        assertThat(result.result.strength).isNull()
        assertThat(saved.resultV2?.summary).isEqualTo("배포 오류를 발견하고 롤백했다.")
        assertThat(saved.status.name).isEqualTo("COMPLETED")
        assertThat(saved.conversationFinishedAt).isNotNull()
    }

    @Test
    fun `완료 요청을 다시 보내면 저장된 결과를 반환하고 AI를 다시 호출하지 않는다`() {
        val started = service.start(userId)
        whenever(resultAIClient.generateResult(any())).thenReturn(generatedResult())

        val first = service.finish(started.retrospectiveId, userId)
        val duplicate = service.finish(started.retrospectiveId, userId)

        assertThat(duplicate).isEqualTo(first)
        verify(resultAIClient, times(1)).generateResult(any())
    }

    @Test
    fun `결과 생성 실패 시 상태를 복구해 같은 완료 요청으로 재시도할 수 있다`() {
        val started = service.start(userId)
        whenever(resultAIClient.generateResult(any()))
            .thenThrow(IllegalStateException("temporary failure"))
            .thenReturn(generatedResult())

        assertThrows<RetrospectiveResultGenerationFailedException> {
            service.finish(started.retrospectiveId, userId)
        }
        val failed = checkNotNull(retrospectiveRepository.findByIdAndUserId(started.retrospectiveId, userId))
        assertThat(failed.conversationStatus?.name).isEqualTo("FINISHED")
        assertThat(failed.summaryGenerationStatus.name).isEqualTo("NOT_STARTED")

        val retried = service.finish(started.retrospectiveId, userId)

        assertThat(retried.resultGenerationStatus.name).isEqualTo("GENERATED")
        verify(resultAIClient, times(2)).generateResult(any())
    }

    @Test
    fun `오래된 결과 생성 상태는 재획득해 완료할 수 있다`() {
        val started = service.start(userId)
        val retrospective = retrospectiveRepository.findByIdAndUserId(started.retrospectiveId, userId)!!
        retrospective.finishConversation()
        retrospective.startV2ResultGeneration(LocalDateTime.now().minusMinutes(11))
        retrospectiveRepository.save(retrospective)
        whenever(resultAIClient.generateResult(any())).thenReturn(generatedResult())

        val result = service.finish(started.retrospectiveId, userId)

        assertThat(result.resultGenerationStatus.name).isEqualTo("GENERATED")
        verify(resultAIClient).generateResult(any())
    }

    @Test
    fun `최근 결과 생성 상태는 중복 완료 요청을 거절한다`() {
        val started = service.start(userId)
        val retrospective = retrospectiveRepository.findByIdAndUserId(started.retrospectiveId, userId)!!
        retrospective.finishConversation()
        retrospective.startV2ResultGeneration(LocalDateTime.now())
        retrospectiveRepository.save(retrospective)

        assertThrows<SummaryGenerationInProgressException> {
            service.finish(started.retrospectiveId, userId)
        }
        verify(resultAIClient, never()).generateResult(any())
    }

    @Test
    fun `삭제된 회고는 결과 생성 요청에서 제외한다`() {
        val started = service.start(userId)
        val retrospective = retrospectiveRepository.findByIdAndUserId(started.retrospectiveId, userId)!!
        retrospective.softDelete()
        retrospectiveRepository.save(retrospective)

        assertThrows<RetrospectiveNotFoundException> {
            service.finish(started.retrospectiveId, userId)
        }
        verify(resultAIClient, never()).generateResult(any())
    }

    private fun generatedResult() =
        GeneratedRetrospectiveResultV2(
            title = "배포 오류 롤백 회고",
            summary = "배포 오류를 발견하고 롤백했다.",
            strength = null,
            improvement = "배포 전 확인이 부족했다.",
            process = "로그를 확인해 원인을 좁혔다.",
            learning = "배포 체크리스트가 필요하다.",
            insight = null,
            nextActions = listOf("배포 체크리스트를 만든다."),
            inputTokens = 120,
            outputTokens = 50,
        )

    private fun offTopicResponse() =
        GeneratedConversationTurn(
            acknowledgement = "요청을 확인했어요.",
            interpretation = "업무 회고와 직접 관련 없는 요청이에요.",
            question = "오늘 실제 업무에서 있었던 일을 들려주시겠어요?",
            questionTarget = RetrospectiveItemType.FACT,
            relevance = MessageRelevance.OFF_TOPIC,
            analysisUpdates = emptyList(),
            inputTokens = 20,
            outputTokens = 10,
        )

    private fun retrospectiveResponse(request: ConversationTurnAIRequest) =
        GeneratedConversationTurn(
            acknowledgement = "배포 자동화를 완료하셨군요.",
            interpretation = "반복 작업을 줄이는 성과가 있었습니다.",
            question = "문제를 해결하는 데 어떤 접근이 가장 효과가 있었나요?",
            questionTarget = RetrospectiveItemType.PROCESS,
            relevance = MessageRelevance.RETROSPECTIVE,
            analysisUpdates =
                listOf(
                    update(RetrospectiveItemType.FACT, request.currentMessageId),
                    update(RetrospectiveItemType.BLOCK, request.currentMessageId),
                    update(RetrospectiveItemType.LEARN, request.currentMessageId),
                    update(RetrospectiveItemType.ACTION, request.currentMessageId),
                ),
            inputTokens = 100,
            outputTokens = 30,
        )

    private fun update(
        itemType: RetrospectiveItemType,
        messageId: UUID,
    ) = ConversationAnalysisUpdate(
        itemType = itemType,
        status = RetrospectiveItemStatus.PARTIAL,
        summary = "$itemType 관련 내용",
        evidenceMessageIds = listOf(messageId),
    )

    @TestConfiguration
    class MetricsConfig {
        @Bean
        fun meterRegistry() = SimpleMeterRegistry()

        @Bean
        fun retrospectiveAiMetrics(meterRegistry: SimpleMeterRegistry) = RetrospectiveAiMetrics(meterRegistry)
    }
}
