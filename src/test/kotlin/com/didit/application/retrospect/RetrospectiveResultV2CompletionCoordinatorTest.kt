package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.required.ChatMessageRepository
import com.didit.application.retrospect.required.GeneratedRetrospectiveResultV2
import com.didit.application.retrospect.required.RetrospectiveAnalysisItemRepository
import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
import com.didit.application.retrospect.required.RetrospectiveConversationTurnRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.RetrospectiveResultV2AIClient
import com.didit.domain.auth.User
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveAnalysisItem
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.RetrospectiveResultDetail
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

class RetrospectiveResultV2CompletionCoordinatorTest {
    @Test
    fun `complete - 보고서체 결과는 한 번 재생성하고 완료 이벤트는 저장 트랜잭션 안에서 발행한다`() {
        val retrospectiveId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val retrospective =
            Retrospective(
                id = retrospectiveId,
                userId = userId,
                flowVersion = com.didit.domain.retrospect.RetrospectiveFlowVersion.V2,
                conversationStatus = com.didit.domain.retrospect.ConversationStatus.ACTIVE,
            )
        val repository = mock<RetrospectiveRepository>()
        val chatMessageRepository = mock<ChatMessageRepository>()
        val attachmentRepository = mock<RetrospectiveAttachmentRepository>()
        val analysisItemRepository = mock<RetrospectiveAnalysisItemRepository>()
        val turnRepository = mock<RetrospectiveConversationTurnRepository>()
        val userFinder = mock<UserFinder>()
        val aiClient = mock<RetrospectiveResultV2AIClient>()
        val eventPublisher = mock<ApplicationEventPublisher>()
        var synchronizationActiveWhenPublished = false

        whenever(repository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(repository.save(any())).thenAnswer { it.arguments[0] }
        whenever(chatMessageRepository.findAllResultEvidenceByRetrospectiveId(retrospectiveId)).thenReturn(emptyList())
        whenever(analysisItemRepository.findAllByRetrospectiveIdOrderByItemTypeAsc(retrospectiveId)).thenReturn(
            listOf(
                analysisItem(retrospectiveId, RetrospectiveItemType.FACT),
                analysisItem(retrospectiveId, RetrospectiveItemType.PROCESS),
                analysisItem(retrospectiveId, RetrospectiveItemType.ACTION),
            ),
        )
        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(mock<User>())
        whenever(aiClient.generateResult(any())).thenReturn(
            generatedResult().copy(summary = "좋다."),
            generatedResult().copy(summary = "좋아요."),
        )
        doAnswer { invocation ->
            if (invocation.getArgument<Any>(0) is RetrospectiveCompletedEvent) {
                synchronizationActiveWhenPublished = TransactionSynchronizationManager.isSynchronizationActive()
            }
            null
        }.whenever(eventPublisher).publishEvent(any<Any>())

        val coordinator =
            RetrospectiveResultV2CompletionCoordinator(
                repository,
                chatMessageRepository,
                attachmentRepository,
                analysisItemRepository,
                turnRepository,
                userFinder,
                aiClient,
                RetrospectiveResultV2TonePolicy(),
                ConversationV2TurnPolicy(),
                TransactionTemplate(TestTransactionManager()),
                RetrospectiveAiMetrics(SimpleMeterRegistry()),
                eventPublisher,
                600,
                30_000,
            )

        coordinator.complete(retrospectiveId, userId)

        verify(aiClient, times(2)).generateResult(any())
        assertThat(retrospective.resultV2?.summary).isEqualTo("좋아요.")
        assertThat(synchronizationActiveWhenPublished).isTrue()
    }

    @Test
    fun `complete - 일시적인 AI 호출 실패는 한 번 재시도하고 결과를 저장한다`() {
        val retrospectiveId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val retrospective =
            Retrospective(
                id = retrospectiveId,
                userId = userId,
                flowVersion = com.didit.domain.retrospect.RetrospectiveFlowVersion.V2,
                conversationStatus = com.didit.domain.retrospect.ConversationStatus.ACTIVE,
            )
        val repository = mock<RetrospectiveRepository>()
        val chatMessageRepository = mock<ChatMessageRepository>()
        val attachmentRepository = mock<RetrospectiveAttachmentRepository>()
        val analysisItemRepository = mock<RetrospectiveAnalysisItemRepository>()
        val turnRepository = mock<RetrospectiveConversationTurnRepository>()
        val userFinder = mock<UserFinder>()
        val aiClient = mock<RetrospectiveResultV2AIClient>()

        whenever(repository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(repository.save(any())).thenAnswer { it.arguments[0] }
        whenever(chatMessageRepository.findAllResultEvidenceByRetrospectiveId(retrospectiveId)).thenReturn(emptyList())
        whenever(analysisItemRepository.findAllByRetrospectiveIdOrderByItemTypeAsc(retrospectiveId)).thenReturn(
            listOf(
                analysisItem(retrospectiveId, RetrospectiveItemType.FACT),
                analysisItem(retrospectiveId, RetrospectiveItemType.PROCESS),
                analysisItem(retrospectiveId, RetrospectiveItemType.ACTION),
            ),
        )
        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(mock<User>())
        whenever(aiClient.generateResult(any()))
            .thenThrow(IllegalStateException("temporary AI failure"))
            .thenReturn(generatedResult())

        val coordinator =
            RetrospectiveResultV2CompletionCoordinator(
                repository,
                chatMessageRepository,
                attachmentRepository,
                analysisItemRepository,
                turnRepository,
                userFinder,
                aiClient,
                RetrospectiveResultV2TonePolicy(),
                ConversationV2TurnPolicy(),
                TransactionTemplate(TestTransactionManager()),
                RetrospectiveAiMetrics(SimpleMeterRegistry()),
                mock<ApplicationEventPublisher>(),
                600,
                30_000,
            )

        coordinator.complete(retrospectiveId, userId)

        verify(aiClient, times(2)).generateResult(any())
        assertThat(retrospective.resultV2?.summary).isEqualTo("배포 자동화를 점검했어요.")
    }

    private fun generatedResult() =
        GeneratedRetrospectiveResultV2(
            title = "배포 자동화 회고",
            summary = "배포 자동화를 점검했어요.",
            strengths = listOf("문제를 빠르게 확인한 점이 돋보임"),
            improvements = listOf("사전 점검의 필요성"),
            processes = listOf("배포 단계를 순서대로 확인하는 방식"),
            learnings = listOf("자동화 검증 방법에 대한 이해"),
            insight = RetrospectiveResultDetail("인사이트", "반복 검증이 중요해요."),
            nextActions = listOf(RetrospectiveResultDetail("다음 행동", "배포 전 점검을 이어갈게요.")),
            inputTokens = 10,
            outputTokens = 5,
        )

    private fun analysisItem(
        retrospectiveId: UUID,
        itemType: RetrospectiveItemType,
    ) = RetrospectiveAnalysisItem(
        retrospectiveId = retrospectiveId,
        itemType = itemType,
        status = RetrospectiveItemStatus.ENOUGH,
        summary = "수집된 내용",
    )

    private class TestTransactionManager : AbstractPlatformTransactionManager() {
        override fun doGetTransaction(): Any = Any()

        override fun doBegin(
            transaction: Any,
            definition: TransactionDefinition,
        ) = Unit

        override fun doCommit(status: DefaultTransactionStatus) = Unit

        override fun doRollback(status: DefaultTransactionStatus) = Unit
    }
}
