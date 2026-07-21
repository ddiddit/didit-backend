package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.InsightResponse
import com.didit.application.retrospect.dto.NextActionResponse
import com.didit.application.retrospect.exception.SummaryGenerationInProgressException
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.SummaryGenerationStatus
import com.didit.domain.shared.Job
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

class RetrospectiveCompletionCoordinatorTest {
    private val repository = mock<RetrospectiveRepository>()
    private val userFinder = mock<UserFinder>()
    private val aiClient = mock<AIClient>()
    private val metrics = mock<RetrospectiveAiMetrics>()
    private val transactionManager = mock<PlatformTransactionManager>()
    private lateinit var coordinator: RetrospectiveCompletionCoordinator

    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        whenever(transactionManager.getTransaction(anyOrNull())).thenReturn(SimpleTransactionStatus())
        whenever(metrics.recordStage<Any>(any(), any(), any())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[2] as () -> Any).invoke()
        }
        coordinator =
            RetrospectiveCompletionCoordinator(
                repository,
                userFinder,
                aiClient,
                TransactionTemplate(transactionManager),
                metrics,
                SummarySaveConcurrencyLimiter(4),
            )
    }

    @Test
    fun `complete - OpenAI 호출은 트랜잭션 밖에서 실행되고 요약을 저장한다`() {
        val retrospective = inProgressRetrospective()
        val summary = summary()
        whenever(repository.findByIdAndUserIdForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(userFinder.getJobByUserId(userId)).thenReturn(Job.DEVELOPER)
        whenever(aiClient.generateSummaryWithTitle(anyOrNull(), any(), anyOrNull())).thenAnswer {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
            summary
        }

        val result = coordinator.complete(retrospectiveId, userId)

        assertThat(result).isEqualTo(summary)
        assertThat(retrospective.summaryGenerationStatus).isEqualTo(SummaryGenerationStatus.GENERATED)
        assertThat(retrospective.summary?.summary).isEqualTo(summary.summary)
        verify(repository, times(2)).save(retrospective)
    }

    @Test
    fun `complete - OpenAI 실패 시 생성 상태를 복구한다`() {
        val retrospective = inProgressRetrospective()
        whenever(repository.findByIdAndUserIdForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(aiClient.generateSummaryWithTitle(anyOrNull(), any(), anyOrNull())).thenThrow(IllegalStateException("failed"))

        assertThrows<IllegalStateException> { coordinator.complete(retrospectiveId, userId) }

        assertThat(retrospective.summaryGenerationStatus).isEqualTo(SummaryGenerationStatus.NOT_STARTED)
    }

    @Test
    fun `complete - 이미 생성 중이면 중복 OpenAI 호출을 차단한다`() {
        val retrospective = inProgressRetrospective().apply { startSummaryGeneration() }
        whenever(repository.findByIdAndUserIdForUpdate(retrospectiveId, userId)).thenReturn(retrospective)

        assertThrows<SummaryGenerationInProgressException> { coordinator.complete(retrospectiveId, userId) }
    }

    private fun inProgressRetrospective() =
        Retrospective(id = retrospectiveId, userId = userId).apply {
            startProgress()
            addMessage(ChatMessage.question(this, "Q1", QuestionType.Q1))
            addMessage(ChatMessage.userAnswer(this, "답변", QuestionType.Q1, InputType.TEXT))
        }

    private fun summary() =
        AISummaryResponse(
            title = "제목",
            summary = "요약",
            blockedPoint = listOf("막힘"),
            solutionProcess = listOf("해결"),
            lessonLearned = listOf("배움"),
            insight = InsightResponse("인사이트", "설명"),
            nextAction = NextActionResponse("행동", "설명"),
            inputTokens = 10,
            outputTokens = 5,
        )
}
