package com.didit.application.retrospect

import com.didit.adapter.config.JpaAuditingConfig
import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.exception.ConversationAiFailedException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.required.ConversationAnalysisUpdate
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.application.retrospect.required.ConversationV2AIClient
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.application.retrospect.required.RetrospectiveAnalysisItemRepository
import com.didit.application.retrospect.required.RetrospectivePolicy
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import com.didit.domain.auth.UserRegisterRequest
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    RetrospectiveConversationV2Service::class,
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

    @MockitoBean
    private lateinit var retrospectiveFinder: RetrospectiveFinder

    @MockitoBean
    private lateinit var retrospectivePolicy: RetrospectivePolicy

    @MockitoBean
    private lateinit var userFinder: UserFinder

    @MockitoBean
    private lateinit var aiClient: ConversationV2AIClient

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
    fun `종료는 대화만 동결하고 회고 결과를 생성하지 않는다`() {
        val started = service.start(userId)

        val result = service.finish(started.retrospectiveId, userId)
        val saved = checkNotNull(retrospectiveRepository.findByIdAndUserId(started.retrospectiveId, userId))

        assertThat(result.conversationStatus.name).isEqualTo("FINISHED")
        assertThat(result.resultGenerationStatus.name).isEqualTo("NOT_STARTED")
        assertThat(saved.summary?.summary).isNull()
        assertThat(saved.conversationFinishedAt).isNotNull()
    }

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
