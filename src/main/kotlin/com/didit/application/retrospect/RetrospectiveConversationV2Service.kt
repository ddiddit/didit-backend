package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.ConversationMessageResult
import com.didit.application.retrospect.dto.ConversationTurnResult
import com.didit.application.retrospect.dto.ConversationV2Result
import com.didit.application.retrospect.dto.FinishConversationV2Result
import com.didit.application.retrospect.dto.StartConversationV2Result
import com.didit.application.retrospect.dto.SubmitConversationMessageResult
import com.didit.application.retrospect.exception.AnotherConversationTurnInProgressException
import com.didit.application.retrospect.exception.ConversationAiFailedException
import com.didit.application.retrospect.exception.ConversationAlreadyFinishedException
import com.didit.application.retrospect.exception.ConversationTurnInProgressException
import com.didit.application.retrospect.exception.DailyLimitExceededException
import com.didit.application.retrospect.exception.DuplicateMessageContentMismatchException
import com.didit.application.retrospect.exception.RetrospectiveFlowVersionMismatchException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.provided.RetrospectiveConversationV2
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.required.ChatMessageRepository
import com.didit.application.retrospect.required.ConversationAnalysisItem
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.application.retrospect.required.ConversationV2AIClient
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.application.retrospect.required.RetrospectiveAnalysisEvidenceRepository
import com.didit.application.retrospect.required.RetrospectiveAnalysisItemRepository
import com.didit.application.retrospect.required.RetrospectiveConversationTurnRepository
import com.didit.application.retrospect.required.RetrospectivePolicy
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationStatus
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveAnalysisEvidence
import com.didit.domain.retrospect.RetrospectiveAnalysisItem
import com.didit.domain.retrospect.RetrospectiveConversationTurn
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import com.didit.domain.retrospect.SummaryGenerationStatus
import com.didit.domain.shared.ServiceTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class RetrospectiveConversationV2Service(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val analysisItemRepository: RetrospectiveAnalysisItemRepository,
    private val evidenceRepository: RetrospectiveAnalysisEvidenceRepository,
    private val turnRepository: RetrospectiveConversationTurnRepository,
    private val retrospectiveFinder: RetrospectiveFinder,
    private val retrospectivePolicy: RetrospectivePolicy,
    private val userFinder: UserFinder,
    private val aiClient: ConversationV2AIClient,
    private val transactionTemplate: TransactionTemplate,
    private val metrics: RetrospectiveAiMetrics,
    private val eventPublisher: ApplicationEventPublisher,
    @param:Value("\${retrospective.v2.max-context-characters:30000}")
    private val maxContextCharacters: Int,
) : RetrospectiveConversationV2 {
    companion object {
        private val logger = LoggerFactory.getLogger(RetrospectiveConversationV2Service::class.java)
        private const val DAILY_LIMIT = 3
    }

    override fun start(userId: UUID): StartConversationV2Result =
        transactionTemplate.execute {
            val user = userFinder.findByIdOrThrow(userId)
            val todayCount = retrospectiveFinder.countByUserIdAndDate(userId, ServiceTime.today())
            if (!retrospectivePolicy.isWhitelisted(user.email) && todayCount >= DAILY_LIMIT) {
                throw DailyLimitExceededException(userId)
            }

            val retrospective = Retrospective.createV2(userId)
            val intro = ChatMessage.v2Intro(retrospective)
            retrospective.addMessage(intro)
            retrospectiveRepository.save(retrospective)
            analysisItemRepository.saveAll(RetrospectiveAnalysisItem.initialize(retrospective.id))

            StartConversationV2Result(
                retrospectiveId = retrospective.id,
                conversationStatus = ConversationStatus.ACTIVE,
                initialMessage = intro.toResult(),
                readyToComplete = false,
            )
        }!!

    override fun submitMessage(
        retrospectiveId: UUID,
        userId: UUID,
        clientMessageId: UUID,
        content: String,
        inputType: InputType,
    ): SubmitConversationMessageResult {
        val preparation =
            metrics.recordStage("conversation_v2", "prepare") {
                prepareTurn(retrospectiveId, userId, clientMessageId, content, inputType)
            }
        preparation.cachedResult?.let {
            metrics.incrementConversationDuplicate()
            return it
        }

        try {
            metrics.recordExternalCallTransactionState("conversation_v2")
            val generated =
                metrics.recordStage("conversation_v2", "openai") {
                    aiClient.generateConversationTurn(checkNotNull(preparation.aiRequest))
                }
            return metrics.recordStage("conversation_v2", "save") {
                saveGeneratedTurn(preparation, generated)
            }
        } catch (exception: Exception) {
            markFailed(preparation.turnId, retrospectiveId)
            metrics.incrementConversationFailure()
            logger.error("V2 회고 AI 응답 생성 실패 - retrospectiveId: $retrospectiveId, turnId: ${preparation.turnId}", exception)
            throw ConversationAiFailedException(retrospectiveId, exception)
        }
    }

    override fun getConversation(
        retrospectiveId: UUID,
        userId: UUID,
    ): ConversationV2Result =
        transactionTemplate.execute {
            val retrospective = findV2(retrospectiveId, userId)
            val items = findOrInitializeItems(retrospectiveId)
            ConversationV2Result(
                retrospectiveId = retrospectiveId,
                conversationStatus = retrospective.conversationStatus ?: ConversationStatus.ACTIVE,
                messages = chatMessageRepository.findAllByRetrospectiveIdOrderByCreatedAtAsc(retrospectiveId).map { it.toResult() },
                turns =
                    turnRepository.findAllByRetrospectiveIdOrderByTurnNumberAsc(retrospectiveId).map {
                        ConversationTurnResult(
                            id = it.id,
                            clientMessageId = it.clientMessageId,
                            userMessageId = it.userMessageId,
                            status = it.status,
                            attemptCount = it.attemptCount,
                            errorCode = it.errorCode,
                        )
                    },
                readyToComplete = calculateReadyToComplete(items),
            )
        }!!

    override fun finish(
        retrospectiveId: UUID,
        userId: UUID,
    ): FinishConversationV2Result {
        var newlyFinished = false
        val result =
            transactionTemplate.execute {
                val retrospective = findV2ForUpdate(retrospectiveId, userId)
                if (retrospective.conversationStatus != ConversationStatus.FINISHED) {
                    if (turnRepository.findFirstByRetrospectiveIdAndStatus(retrospectiveId, ConversationTurnStatus.PROCESSING) != null) {
                        throw AnotherConversationTurnInProgressException(retrospectiveId)
                    }
                    retrospective.finishConversation()
                    retrospectiveRepository.save(retrospective)
                    newlyFinished = true
                }
                FinishConversationV2Result(
                    retrospectiveId = retrospectiveId,
                    conversationStatus = ConversationStatus.FINISHED,
                    resultGenerationStatus = SummaryGenerationStatus.NOT_STARTED,
                )
            }!!
        if (newlyFinished) eventPublisher.publishEvent(RetrospectiveConversationFinishedEvent(retrospectiveId, userId))
        return result
    }

    private fun prepareTurn(
        retrospectiveId: UUID,
        userId: UUID,
        clientMessageId: UUID,
        content: String,
        inputType: InputType,
    ): TurnPreparation =
        transactionTemplate.execute {
            val retrospective = findV2ForUpdate(retrospectiveId, userId)
            if (!retrospective.isConversationActive()) throw ConversationAlreadyFinishedException(retrospectiveId)

            val existing = turnRepository.findByRetrospectiveIdAndClientMessageId(retrospectiveId, clientMessageId)
            if (existing != null) {
                val userMessage = chatMessageRepository.findById(existing.userMessageId) ?: error("사용자 메시지를 찾을 수 없습니다.")
                if (userMessage.content != content || userMessage.inputType != inputType) {
                    throw DuplicateMessageContentMismatchException(clientMessageId)
                }
                when (existing.status) {
                    ConversationTurnStatus.PROCESSING -> throw ConversationTurnInProgressException(retrospectiveId)
                    ConversationTurnStatus.COMPLETED ->
                        return@execute TurnPreparation.cached(
                            retrospectiveId = retrospectiveId,
                            userId = userId,
                            existing.toSubmitResult(
                                assistantMessage =
                                    chatMessageRepository.findById(checkNotNull(existing.assistantMessageId))
                                        ?: error("AI 메시지를 찾을 수 없습니다."),
                                readyToComplete = calculateReadyToComplete(findOrInitializeItems(retrospectiveId)),
                            ),
                        )
                    ConversationTurnStatus.FAILED -> {
                        existing.retry()
                        turnRepository.save(existing)
                        metrics.incrementConversationRetry()
                        return@execute createPreparation(retrospective, existing, userMessage)
                    }
                }
            }

            if (turnRepository.findFirstByRetrospectiveIdAndStatus(retrospectiveId, ConversationTurnStatus.PROCESSING) != null) {
                metrics.incrementConversationConflict()
                throw AnotherConversationTurnInProgressException(retrospectiveId)
            }

            if (retrospective.isPending()) retrospective.startProgress()
            val userMessage = ChatMessage.v2UserMessage(retrospective, content, inputType)
            retrospective.addMessage(userMessage)
            retrospectiveRepository.save(retrospective)
            val turn =
                turnRepository.save(
                    RetrospectiveConversationTurn(
                        retrospectiveId = retrospectiveId,
                        clientMessageId = clientMessageId,
                        userMessageId = userMessage.id,
                        turnNumber = turnRepository.countByRetrospectiveId(retrospectiveId) + 1,
                    ),
                )
            createPreparation(retrospective, turn, userMessage)
        }!!

    private fun createPreparation(
        retrospective: Retrospective,
        turn: RetrospectiveConversationTurn,
        userMessage: ChatMessage,
    ): TurnPreparation {
        val user = userFinder.findByIdOrThrow(retrospective.userId)
        val messages = chatMessageRepository.findAllByRetrospectiveIdOrderByCreatedAtAsc(retrospective.id)
        val items = findOrInitializeItems(retrospective.id)
        val context = trimContext(messages.map { ConversationContextMessage(it.id, it.sender, it.content) })
        return TurnPreparation(
            retrospectiveId = retrospective.id,
            userId = retrospective.userId,
            turnId = turn.id,
            userMessageId = userMessage.id,
            aiRequest =
                ConversationTurnAIRequest(
                    job = user.job,
                    experience = user.experience,
                    messages = context,
                    analysisItems = items.map { ConversationAnalysisItem(it.itemType, it.status, it.summary) },
                    currentMessageId = userMessage.id,
                    consecutiveIrrelevantCount = consecutiveIrrelevantCount(messages, userMessage.id),
                ),
        )
    }

    private fun saveGeneratedTurn(
        preparation: TurnPreparation,
        generated: GeneratedConversationTurn,
    ): SubmitConversationMessageResult =
        transactionTemplate.execute {
            val retrospective =
                retrospectiveRepository.findByIdAndUserIdForUpdate(preparation.retrospectiveId, preparation.userId)
                    ?: throw RetrospectiveNotFoundException(preparation.retrospectiveId)
            val turn =
                turnRepository
                    .findAllByRetrospectiveIdOrderByTurnNumberAsc(preparation.retrospectiveId)
                    .find { it.id == preparation.turnId } ?: error("대화 턴을 찾을 수 없습니다.")
            check(turn.status == ConversationTurnStatus.PROCESSING)
            val userMessage = chatMessageRepository.findById(preparation.userMessageId) ?: error("사용자 메시지를 찾을 수 없습니다.")
            userMessage.classify(generated.relevance)
            chatMessageRepository.save(userMessage)

            val responseContent = generated.content()
            check(responseContent.isNotBlank()) { "AI 응답이 비어 있습니다." }
            val assistantMessage =
                chatMessageRepository.save(
                    ChatMessage.v2AssistantMessage(
                        retrospective = retrospective,
                        content = responseContent,
                        systemGuide =
                            generated.relevance == MessageRelevance.OFF_TOPIC || generated.relevance == MessageRelevance.SERVICE_HELP,
                    ),
                )

            if (generated.relevance == MessageRelevance.RETROSPECTIVE) {
                applyAnalysisUpdates(preparation.retrospectiveId, generated)
            }
            turn.complete(assistantMessage.id, generated.questionTarget, generated.inputTokens, generated.outputTokens)
            turnRepository.save(turn)
            retrospective.addTokens(generated.inputTokens, generated.outputTokens)
            retrospectiveRepository.save(retrospective)
            metrics.incrementConversationRelevance(generated.relevance)

            val ready = calculateReadyToComplete(findOrInitializeItems(preparation.retrospectiveId))
            SubmitConversationMessageResult(
                turnId = turn.id,
                userMessageId = userMessage.id,
                assistantMessage = assistantMessage.toResult(),
                readyToComplete = ready,
            )
        }!!

    private fun applyAnalysisUpdates(
        retrospectiveId: UUID,
        generated: GeneratedConversationTurn,
    ) {
        val items = findOrInitializeItems(retrospectiveId).associateBy { it.itemType }
        val validMessages =
            chatMessageRepository
                .findAllByRetrospectiveIdOrderByCreatedAtAsc(retrospectiveId)
                .filter { it.sender == Sender.USER && it.includedInResult }
                .associateBy { it.id }
        generated.analysisUpdates.forEach { update ->
            val evidenceIds = update.evidenceMessageIds.filter { it in validMessages }
            if (evidenceIds.isEmpty()) return@forEach
            val item = items[update.itemType] ?: return@forEach
            item.update(update.status, update.summary)
            analysisItemRepository.save(item)
            val newEvidence =
                evidenceIds
                    .filterNot { evidenceRepository.existsByAnalysisItemIdAndMessageId(item.id, it) }
                    .map { RetrospectiveAnalysisEvidence(analysisItemId = item.id, messageId = it) }
            if (newEvidence.isNotEmpty()) evidenceRepository.saveAll(newEvidence)
        }
    }

    private fun markFailed(
        turnId: UUID,
        retrospectiveId: UUID,
    ) {
        runCatching {
            transactionTemplate.executeWithoutResult {
                turnRepository
                    .findAllByRetrospectiveIdOrderByTurnNumberAsc(retrospectiveId)
                    .find { it.id == turnId && it.status == ConversationTurnStatus.PROCESSING }
                    ?.also {
                        it.fail("AI_RESPONSE_GENERATION_FAILED")
                        turnRepository.save(it)
                    }
            }
        }.onFailure { logger.error("V2 회고 실패 상태 저장 실패 - retrospectiveId: $retrospectiveId, turnId: $turnId", it) }
    }

    private fun findV2(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective {
        val retrospective =
            retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)
                ?: throw RetrospectiveNotFoundException(retrospectiveId)
        if (!retrospective.isV2()) throw RetrospectiveFlowVersionMismatchException(retrospectiveId)
        return retrospective
    }

    private fun findV2ForUpdate(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective {
        val retrospective =
            retrospectiveRepository.findByIdAndUserIdForUpdate(retrospectiveId, userId)
                ?: throw RetrospectiveNotFoundException(retrospectiveId)
        if (!retrospective.isV2()) throw RetrospectiveFlowVersionMismatchException(retrospectiveId)
        return retrospective
    }

    private fun findOrInitializeItems(retrospectiveId: UUID): List<RetrospectiveAnalysisItem> {
        val existing = analysisItemRepository.findAllByRetrospectiveIdOrderByItemTypeAsc(retrospectiveId)
        if (existing.size == RetrospectiveItemType.entries.size) return existing
        val existingTypes = existing.map { it.itemType }.toSet()
        val created = RetrospectiveAnalysisItem.initialize(retrospectiveId).filterNot { it.itemType in existingTypes }
        return existing + analysisItemRepository.saveAll(created)
    }

    private fun calculateReadyToComplete(items: List<RetrospectiveAnalysisItem>): Boolean {
        val statuses = items.associate { it.itemType to it.status }

        fun collected(type: RetrospectiveItemType) = statuses[type] != null && statuses[type] != RetrospectiveItemStatus.EMPTY
        return collected(RetrospectiveItemType.FACT) &&
            statuses.values.count { it != RetrospectiveItemStatus.EMPTY } >= 4 &&
            listOf(RetrospectiveItemType.STRENGTH, RetrospectiveItemType.BLOCK, RetrospectiveItemType.PROCESS).any(::collected) &&
            listOf(RetrospectiveItemType.LEARN, RetrospectiveItemType.ACTION).any(::collected)
    }

    private fun consecutiveIrrelevantCount(
        messages: List<ChatMessage>,
        currentMessageId: UUID,
    ): Int =
        messages
            .filter { it.sender == Sender.USER && it.id != currentMessageId }
            .asReversed()
            .takeWhile { it.relevance != MessageRelevance.RETROSPECTIVE }
            .count { it.relevance != null }

    private fun trimContext(messages: List<ConversationContextMessage>): List<ConversationContextMessage> {
        var used = 0
        return messages
            .asReversed()
            .takeWhile {
                used += it.content.length
                used <= maxContextCharacters
            }.asReversed()
    }

    private fun ChatMessage.toResult(): ConversationMessageResult =
        if (messageType == ConversationMessageType.INTRO) {
            ConversationMessageResult(
                id = id,
                sender = sender,
                messageType = messageType,
                title = content,
                body = supportingContent,
                createdAt = createdAt,
            )
        } else {
            ConversationMessageResult(
                id = id,
                sender = sender,
                messageType = messageType,
                content = content,
                createdAt = createdAt,
            )
        }

    private fun RetrospectiveConversationTurn.toSubmitResult(
        assistantMessage: ChatMessage,
        readyToComplete: Boolean,
    ): SubmitConversationMessageResult =
        SubmitConversationMessageResult(
            turnId = id,
            userMessageId = userMessageId,
            assistantMessage = assistantMessage.toResult(),
            readyToComplete = readyToComplete,
        )
}

private data class TurnPreparation(
    val retrospectiveId: UUID,
    val userId: UUID,
    val turnId: UUID,
    val userMessageId: UUID,
    val aiRequest: ConversationTurnAIRequest?,
    val cachedResult: SubmitConversationMessageResult? = null,
) {
    companion object {
        fun cached(
            retrospectiveId: UUID,
            userId: UUID,
            result: SubmitConversationMessageResult,
        ): TurnPreparation =
            TurnPreparation(
                retrospectiveId = retrospectiveId,
                userId = userId,
                turnId = result.turnId,
                userMessageId = result.userMessageId,
                aiRequest = null,
                cachedResult = result,
            )
    }
}
