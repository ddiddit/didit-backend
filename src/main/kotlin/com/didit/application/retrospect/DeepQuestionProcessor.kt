package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.QuestionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class DeepQuestionProcessor(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val userFinder: UserFinder,
    private val aiClient: AIClient,
    private val transactionTemplate: TransactionTemplate,
    private val metrics: RetrospectiveAiMetrics,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DeepQuestionProcessor::class.java)
        const val DEFAULT_FALLBACK_QUESTION = "오늘 회고를 통해 어떤 성찰을 얻으셨나요?"
    }

    fun process(event: DeepQuestionGenerationEvent) {
        val snapshot =
            transactionTemplate.execute {
                val retrospective =
                    retrospectiveRepository.findByIdAndUserId(event.retrospectiveId, event.userId)
                        ?: throw RetrospectiveNotFoundException(event.retrospectiveId)
                if (!retrospective.canAddDeepQuestion()) return@execute null
                DeepQuestionSnapshot(
                    job = userFinder.getJobByUserId(event.userId),
                    answers = retrospective.getAnswersUpToQ3(),
                )
            } ?: return

        val generated =
            runCatching {
                metrics.recordExternalCallTransactionState("deep_question")
                metrics.recordStage("deep_question", "openai") {
                    aiClient.generateDeepQuestion(snapshot.job, snapshot.answers)
                }
            }.onFailure {
                logger.error("심화 질문 생성 실패 - retrospectiveId: ${event.retrospectiveId}", it)
            }.getOrNull()

        saveQuestion(event, generated?.content ?: DEFAULT_FALLBACK_QUESTION, generated?.inputTokens ?: 0, generated?.outputTokens ?: 0)
    }

    fun saveFallback(event: DeepQuestionGenerationEvent) {
        saveQuestion(event, DEFAULT_FALLBACK_QUESTION, 0, 0)
    }

    private fun saveQuestion(
        event: DeepQuestionGenerationEvent,
        content: String,
        inputTokens: Int,
        outputTokens: Int,
    ) {
        transactionTemplate.executeWithoutResult {
            val retrospective =
                retrospectiveRepository.findByIdAndUserIdForUpdate(event.retrospectiveId, event.userId)
                    ?: throw RetrospectiveNotFoundException(event.retrospectiveId)
            if (!retrospective.canAddDeepQuestion()) return@executeWithoutResult
            retrospective.addMessage(
                ChatMessage.question(retrospective, content, QuestionType.Q4_DEEP),
            )
            retrospective.addTokens(inputTokens, outputTokens)
            retrospectiveRepository.save(retrospective)
        }
    }
}

private data class DeepQuestionSnapshot(
    val job: com.didit.domain.shared.Job?,
    val answers: List<String>,
)
