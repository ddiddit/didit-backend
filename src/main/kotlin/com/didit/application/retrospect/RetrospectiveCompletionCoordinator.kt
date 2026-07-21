package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.exception.RetrospectiveAlreadyCompletedException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.exception.RetrospectiveNotInProgressException
import com.didit.application.retrospect.exception.SummaryAlreadyGeneratedException
import com.didit.application.retrospect.exception.SummaryGenerationInProgressException
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.domain.retrospect.Sender
import com.didit.domain.retrospect.SummaryGenerationStatus
import com.didit.domain.shared.Job
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Component
class RetrospectiveCompletionCoordinator(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val userFinder: UserFinder,
    private val aiClient: AIClient,
    private val transactionTemplate: TransactionTemplate,
    private val metrics: RetrospectiveAiMetrics,
    private val summarySaveConcurrencyLimiter: SummarySaveConcurrencyLimiter,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RetrospectiveCompletionCoordinator::class.java)
    }

    fun complete(
        retrospectiveId: UUID,
        userId: UUID,
    ): AISummaryResponse =
        metrics.recordStage("summary", "total") {
            val snapshot = metrics.recordStage("summary", "snapshot") { prepare(retrospectiveId, userId) }
            try {
                metrics.recordExternalCallTransactionState("summary")
                val summary =
                    metrics.recordStage("summary", "openai") {
                        aiClient.generateSummaryWithTitle(snapshot.job, snapshot.answers, snapshot.deepQuestion)
                    }
                metrics.recordStage("summary", "save") {
                    summarySaveConcurrencyLimiter.execute {
                        save(retrospectiveId, userId, summary)
                    }
                }
                summary
            } catch (exception: Exception) {
                reset(retrospectiveId, userId)
                logger.error("회고 요약 생성 실패 - userId: $userId, retrospectiveId: $retrospectiveId", exception)
                throw exception
            }
        }

    private fun prepare(
        retrospectiveId: UUID,
        userId: UUID,
    ): CompletionSnapshot =
        transactionTemplate.execute {
            val retrospective =
                retrospectiveRepository.findByIdAndUserIdForUpdate(retrospectiveId, userId)
                    ?: throw RetrospectiveNotFoundException(retrospectiveId)
            if (retrospective.isCompleted()) throw RetrospectiveAlreadyCompletedException(retrospectiveId)
            if (retrospective.isDeleted()) throw RetrospectiveNotInProgressException(retrospectiveId)
            when (retrospective.summaryGenerationStatus) {
                SummaryGenerationStatus.GENERATING -> throw SummaryGenerationInProgressException(retrospectiveId)
                SummaryGenerationStatus.GENERATED -> throw SummaryAlreadyGeneratedException(retrospectiveId)
                SummaryGenerationStatus.NOT_STARTED -> retrospective.startSummaryGeneration()
            }
            retrospectiveRepository.save(retrospective)
            CompletionSnapshot(
                job = userFinder.getJobByUserId(userId),
                answers = retrospective.getAllAnswers(),
                deepQuestion =
                    retrospective.chatMessages
                        .find { it.questionType == QuestionType.Q4_DEEP && it.sender == Sender.AI }
                        ?.content,
            )
        }!!

    private fun save(
        retrospectiveId: UUID,
        userId: UUID,
        summary: AISummaryResponse,
    ) {
        transactionTemplate.executeWithoutResult {
            val retrospective =
                retrospectiveRepository.findByIdAndUserIdForUpdate(retrospectiveId, userId)
                    ?: throw RetrospectiveNotFoundException(retrospectiveId)
            if (retrospective.summaryGenerationStatus != SummaryGenerationStatus.GENERATING) {
                throw SummaryGenerationInProgressException(retrospectiveId)
            }
            retrospective.saveSummary(
                RetrospectiveSummary(
                    summary = summary.summary,
                    blockedPoint = summary.blockedPoint,
                    solutionProcess = summary.solutionProcess,
                    lessonLearned = summary.lessonLearned,
                    insightTitle = summary.insight.title,
                    insightDescription = summary.insight.description,
                    nextActionTitle = summary.nextAction.title,
                    nextActionDescription = summary.nextAction.description,
                ),
            )
            retrospective.addTokens(summary.inputTokens, summary.outputTokens)
            retrospectiveRepository.save(retrospective)
        }
    }

    private fun reset(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        runCatching {
            transactionTemplate.executeWithoutResult {
                retrospectiveRepository.findByIdAndUserIdForUpdate(retrospectiveId, userId)?.let {
                    it.resetSummaryGeneration()
                    retrospectiveRepository.save(it)
                }
            }
        }.onFailure { logger.error("AI 요약 생성 상태 복구 실패 - retrospectiveId: $retrospectiveId", it) }
    }
}

private data class CompletionSnapshot(
    val job: Job?,
    val answers: List<String>,
    val deepQuestion: String?,
)
