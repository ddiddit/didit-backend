package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.FinishConversationV2Result
import com.didit.application.retrospect.dto.RetrospectiveResultV2Result
import com.didit.application.retrospect.exception.AnotherConversationTurnInProgressException
import com.didit.application.retrospect.exception.RetrospectiveFlowVersionMismatchException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.exception.RetrospectiveResultGenerationFailedException
import com.didit.application.retrospect.exception.SummaryGenerationInProgressException
import com.didit.application.retrospect.required.ChatMessageRepository
import com.didit.application.retrospect.required.ConversationAnalysisItem
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.GeneratedRetrospectiveResultV2
import com.didit.application.retrospect.required.RetrospectiveAnalysisItemRepository
import com.didit.application.retrospect.required.RetrospectiveConversationTurnRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.RetrospectiveResultV2AIClient
import com.didit.application.retrospect.required.RetrospectiveResultV2AIRequest
import com.didit.domain.retrospect.ConversationStatus
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import com.didit.domain.retrospect.RetrospectiveResultV2
import com.didit.domain.retrospect.SummaryGenerationStatus
import com.didit.domain.shared.ServiceTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Component
class RetrospectiveResultV2CompletionCoordinator(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val analysisItemRepository: RetrospectiveAnalysisItemRepository,
    private val turnRepository: RetrospectiveConversationTurnRepository,
    private val userFinder: UserFinder,
    private val aiClient: RetrospectiveResultV2AIClient,
    private val transactionTemplate: TransactionTemplate,
    private val metrics: RetrospectiveAiMetrics,
    private val eventPublisher: ApplicationEventPublisher,
    @param:Value("\${retrospective.v2.result-generation-timeout-seconds:600}")
    private val resultGenerationTimeoutSeconds: Long,
    @param:Value("\${retrospective.v2.max-context-characters:30000}")
    private val maxContextCharacters: Int,
) {
    fun complete(
        retrospectiveId: UUID,
        userId: UUID,
    ): FinishConversationV2Result =
        metrics.recordStage("result_v2", "total") {
            val preparation = metrics.recordStage("result_v2", "prepare") { prepare(retrospectiveId, userId) }
            preparation.cached?.let { return@recordStage it }

            try {
                metrics.recordExternalCallTransactionState("result_v2")
                val generated =
                    metrics.recordStage("result_v2", "openai") {
                        aiClient.generateResult(checkNotNull(preparation.request))
                    }
                val result =
                    metrics.recordStage("result_v2", "save") {
                        save(retrospectiveId, userId, checkNotNull(preparation.attemptId), generated)
                    }
                result
            } catch (exception: Exception) {
                reset(retrospectiveId, userId, preparation.attemptId)
                logger.error("V2 회고 결과 생성 실패 - retrospectiveId: $retrospectiveId", exception)
                throw RetrospectiveResultGenerationFailedException(retrospectiveId, exception)
            }
        }

    private fun prepare(
        retrospectiveId: UUID,
        userId: UUID,
    ): ResultPreparation =
        transactionTemplate.execute {
            val retrospective = findV2ForUpdate(retrospectiveId, userId)
            if (retrospective.summaryGenerationStatus == SummaryGenerationStatus.GENERATED) {
                return@execute ResultPreparation(cached = retrospective.toFinishResult())
            }
            val now = LocalDateTime.now()
            if (retrospective.summaryGenerationStatus == SummaryGenerationStatus.GENERATING &&
                !retrospective.isV2ResultGenerationStale(now, Duration.ofSeconds(resultGenerationTimeoutSeconds))
            ) {
                throw SummaryGenerationInProgressException(retrospectiveId)
            }
            if (turnRepository.findFirstByRetrospectiveIdAndStatus(retrospectiveId, ConversationTurnStatus.PROCESSING) != null) {
                throw AnotherConversationTurnInProgressException(retrospectiveId)
            }

            retrospective.finishConversation()
            val attemptId = UUID.randomUUID()
            retrospective.startV2ResultGeneration(now, attemptId)
            retrospectiveRepository.save(retrospective)
            val user = userFinder.findByIdOrThrow(userId)
            ResultPreparation(
                request =
                    RetrospectiveResultV2AIRequest(
                        job = user.job,
                        experience = user.experience,
                        messages =
                            trimContext(
                                chatMessageRepository
                                    .findAllResultEvidenceByRetrospectiveId(retrospectiveId)
                                    .map { ConversationContextMessage(it.id, it.sender, it.content) },
                            ),
                        analysisItems =
                            analysisItemRepository
                                .findAllByRetrospectiveIdOrderByItemTypeAsc(retrospectiveId)
                                .map { ConversationAnalysisItem(it.itemType, it.status, it.summary) },
                    ),
                attemptId = attemptId,
            )
        }!!

    private fun trimContext(messages: List<ConversationContextMessage>): List<ConversationContextMessage> {
        var used = 0
        return messages
            .asReversed()
            .takeWhile {
                used += it.content.length
                used <= maxContextCharacters
            }.asReversed()
    }

    private fun save(
        retrospectiveId: UUID,
        userId: UUID,
        attemptId: UUID,
        generated: GeneratedRetrospectiveResultV2,
    ): FinishConversationV2Result =
        transactionTemplate.execute {
            val retrospective = findV2ForUpdate(retrospectiveId, userId)
            if (retrospective.summaryGenerationStatus != SummaryGenerationStatus.GENERATING) {
                throw SummaryGenerationInProgressException(retrospectiveId)
            }
            if (retrospective.resultGenerationAttemptId != attemptId) {
                throw SummaryGenerationInProgressException(retrospectiveId)
            }
            retrospective.saveV2Result(
                title = generated.title,
                result =
                    RetrospectiveResultV2(
                        summary = generated.summary,
                        strengths = generated.strengths,
                        improvements = generated.improvements,
                        processes = generated.processes,
                        learnings = generated.learnings,
                        insight = generated.insight,
                        nextActions = generated.nextActions,
                    ),
            )
            retrospective.addTokens(generated.inputTokens, generated.outputTokens)
            retrospectiveRepository.save(retrospective)
            publishCompletedEvent(retrospectiveId, userId)
            retrospective.toFinishResult()
        }!!

    private fun reset(
        retrospectiveId: UUID,
        userId: UUID,
        attemptId: UUID?,
    ) {
        runCatching {
            transactionTemplate.executeWithoutResult {
                retrospectiveRepository.findByIdAndUserIdForUpdate(retrospectiveId, userId)?.let {
                    if (it.resultGenerationAttemptId == attemptId) {
                        it.resetSummaryGeneration()
                        retrospectiveRepository.save(it)
                    }
                }
            }
        }.onFailure { logger.error("V2 회고 결과 생성 상태 복구 실패 - retrospectiveId: $retrospectiveId", it) }
    }

    private fun findV2ForUpdate(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective {
        val retrospective =
            retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)
                ?: throw RetrospectiveNotFoundException(retrospectiveId)
        if (!retrospective.isV2()) throw RetrospectiveFlowVersionMismatchException(retrospectiveId)
        return retrospective
    }

    private fun Retrospective.toFinishResult(): FinishConversationV2Result {
        val stored = checkNotNull(resultV2) { "생성된 V2 결과가 없습니다." }
        return FinishConversationV2Result(
            retrospectiveId = id,
            conversationStatus = conversationStatus ?: ConversationStatus.FINISHED,
            resultGenerationStatus = summaryGenerationStatus,
            title = checkNotNull(title),
            result =
                RetrospectiveResultV2Result(
                    summary = stored.summary,
                    strengths = stored.strengths,
                    improvements = stored.improvements,
                    processes = stored.processes,
                    learnings = stored.learnings,
                    insight = stored.insight,
                    nextActions = stored.nextActions,
                ),
        )
    }

    private fun publishCompletedEvent(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        runCatching {
            eventPublisher.publishEvent(
                RetrospectiveCompletedEvent(
                    userId = userId,
                    retrospectiveId = retrospectiveId,
                    retroDate = ServiceTime.today(),
                ),
            )
        }.onFailure { logger.error("V2 회고 완료 이벤트 발행 실패 - retrospectiveId: $retrospectiveId", it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RetrospectiveResultV2CompletionCoordinator::class.java)
    }
}

private data class ResultPreparation(
    val request: RetrospectiveResultV2AIRequest? = null,
    val cached: FinishConversationV2Result? = null,
    val attemptId: UUID? = null,
)
