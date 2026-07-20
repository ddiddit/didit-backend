package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.GeneratedDeepQuestion
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

class DeepQuestionProcessorTest {
    private val repository = mock<RetrospectiveRepository>()
    private val userFinder = mock<UserFinder>()
    private val aiClient = mock<AIClient>()
    private val metrics = mock<RetrospectiveAiMetrics>()
    private val transactionManager = mock<PlatformTransactionManager>()
    private lateinit var processor: DeepQuestionProcessor

    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        whenever(transactionManager.getTransaction(anyOrNull())).thenReturn(SimpleTransactionStatus())
        whenever(metrics.recordStage<Any>(any(), any(), any())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[2] as () -> Any).invoke()
        }
        processor = DeepQuestionProcessor(repository, userFinder, aiClient, TransactionTemplate(transactionManager), metrics)
    }

    @Test
    fun `process - 심화 질문과 토큰을 한 번만 저장한다`() {
        val retrospective = retrospectiveReadyForDeepQuestion()
        whenever(repository.findByIdAndUserId(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(repository.findByIdAndUserIdForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(aiClient.generateDeepQuestion(anyOrNull(), any())).thenReturn(GeneratedDeepQuestion("생성 질문", 10, 3))

        processor.process(DeepQuestionGenerationEvent(retrospectiveId, userId))
        processor.process(DeepQuestionGenerationEvent(retrospectiveId, userId))

        assertThat(retrospective.hasDeepQuestion()).isTrue()
        assertThat(retrospective.inputTokens).isEqualTo(10)
        assertThat(retrospective.outputTokens).isEqualTo(3)
        verify(repository).save(retrospective)
    }

    @Test
    fun `process - AI 실패 시 기본 질문을 저장한다`() {
        val retrospective = retrospectiveReadyForDeepQuestion()
        whenever(repository.findByIdAndUserId(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(repository.findByIdAndUserIdForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(aiClient.generateDeepQuestion(anyOrNull(), any())).thenThrow(IllegalStateException("failed"))

        processor.process(DeepQuestionGenerationEvent(retrospectiveId, userId))

        assertThat(retrospective.hasDeepQuestion()).isTrue()
        assertThat(retrospective.chatMessages.last().content).isEqualTo(DeepQuestionProcessor.DEFAULT_FALLBACK_QUESTION)
    }

    private fun retrospectiveReadyForDeepQuestion() =
        Retrospective(id = retrospectiveId, userId = userId).apply {
            startProgress()
            listOf(QuestionType.Q1, QuestionType.Q2, QuestionType.Q3).forEach {
                addMessage(ChatMessage.question(this, it.name, it))
                addMessage(ChatMessage.userAnswer(this, "${it.name} 답변", it, InputType.TEXT))
            }
        }
}
