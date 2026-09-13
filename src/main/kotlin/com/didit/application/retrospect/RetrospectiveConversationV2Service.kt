package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.ConversationMessageAttachmentResult
import com.didit.application.retrospect.dto.ConversationMessageResult
import com.didit.application.retrospect.dto.ConversationTurnResult
import com.didit.application.retrospect.dto.ConversationV2Result
import com.didit.application.retrospect.dto.FinishConversationV2Result
import com.didit.application.retrospect.dto.StartConversationV2Result
import com.didit.application.retrospect.dto.SubmitConversationMessageResult
import com.didit.application.retrospect.exception.AnotherConversationTurnInProgressException
import com.didit.application.retrospect.exception.AttachmentLimitExceededException
import com.didit.application.retrospect.exception.AttachmentNotFoundException
import com.didit.application.retrospect.exception.AttachmentNotUploadedException
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
import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
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
    private val attachmentRepository: RetrospectiveAttachmentRepository,
    private val retrospectiveFinder: RetrospectiveFinder,
    private val retrospectivePolicy: RetrospectivePolicy,
    private val userFinder: UserFinder,
    private val aiClient: ConversationV2AIClient,
    private val transactionTemplate: TransactionTemplate,
    private val metrics: RetrospectiveAiMetrics,
    private val resultCompletionCoordinator: RetrospectiveResultV2CompletionCoordinator,
    private val eventPublisher: ApplicationEventPublisher,
    private val sensitiveDataDetector: AttachmentSensitiveDataDetector,
    private val turnPolicy: ConversationV2TurnPolicy,
    @param:Value("\${retrospective.v2.max-context-characters:30000}")
    private val maxContextCharacters: Int,
) : RetrospectiveConversationV2 {
    companion object {
        private val logger = LoggerFactory.getLogger(RetrospectiveConversationV2Service::class.java)
        private const val DAILY_LIMIT = 3
        private const val SENSITIVE_DATA_WARNING =
            "첨부파일에 개인정보나 민감정보가 포함되어 있을 수 있어요. 안전을 위해 해당 파일을 삭제해 주세요."
        private const val UNREADABLE_ATTACHMENT_GUIDE =
            "첨부하신 파일을 여는 데 문제가 있었어요. 다시 첨부해 주시거나, " +
                "어떤 내용이었는지 간단히 말씀해 주셔도 괜찮아요."
    }

    override fun start(userId: UUID): StartConversationV2Result =
        transactionTemplate.execute {
            val user = userFinder.findByIdOrThrow(userId)
            val todayCount = retrospectiveFinder.countByUserIdAndDate(userId, ServiceTime.today())
            if (!retrospectivePolicy.isWhitelisted(user.email) && todayCount >= DAILY_LIMIT) {
                throw DailyLimitExceededException(userId)
            }

            val retrospective = Retrospective.createV2(userId)
            val introSnapshot = turnPolicy.initialIntro()
            val intro =
                ChatMessage.v2Intro(
                    retrospective = retrospective,
                    content = introSnapshot.content,
                    supportingContent = introSnapshot.supportingContent,
                )
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
        attachmentIds: List<UUID>,
    ): SubmitConversationMessageResult {
        val preparation =
            metrics.recordStage("conversation_v2", "prepare") {
                prepareTurn(retrospectiveId, userId, clientMessageId, content, inputType, attachmentIds)
            }
        preparation.cachedResult?.let {
            metrics.incrementConversationDuplicate()
            return it
        }

        if (preparation.attachmentIds.isNotEmpty()) {
            eventPublisher.publishEvent(
                AttachmentConversationRequestedEvent(
                    retrospectiveId = preparation.retrospectiveId,
                    userId = preparation.userId,
                    turnId = preparation.turnId,
                    userMessageId = preparation.userMessageId,
                ),
            )
            return SubmitConversationMessageResult(
                turnId = preparation.turnId,
                userMessageId = preparation.userMessageId,
                assistantMessage = null,
                readyToComplete = false,
            )
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
                messages =
                    chatMessageRepository.findAllByRetrospectiveIdOrderByCreatedAtAsc(retrospectiveId).map {
                        it.toResult(attachmentRepository.findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(it.id))
                    },
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
    ): FinishConversationV2Result = resultCompletionCoordinator.complete(retrospectiveId, userId)

    fun processAttachedTurn(event: AttachmentConversationRequestedEvent) {
        val preparation =
            transactionTemplate.execute {
                val retrospective = findV2(event.retrospectiveId, event.userId)
                val turn =
                    turnRepository.findAllByRetrospectiveIdOrderByTurnNumberAsc(event.retrospectiveId).find { it.id == event.turnId }
                        ?: error("대화 턴을 찾을 수 없습니다.")
                check(turn.status == ConversationTurnStatus.PROCESSING)
                val userMessage = chatMessageRepository.findById(event.userMessageId) ?: error("사용자 메시지를 찾을 수 없습니다.")
                val attachments = attachmentRepository.findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(userMessage.id)
                check(
                    attachments.isNotEmpty() &&
                        attachments.all {
                            it.analysisStatus in
                                setOf(
                                    com.didit.domain.retrospect.AttachmentAnalysisStatus.COMPLETED,
                                    com.didit.domain.retrospect.AttachmentAnalysisStatus.UNREADABLE,
                                )
                        } &&
                        (
                            userMessage.content.isNotBlank() ||
                                attachments.any {
                                    it.analysisStatus == com.didit.domain.retrospect.AttachmentAnalysisStatus.COMPLETED
                                }
                        ),
                )
                createPreparation(retrospective, turn, userMessage, attachments.map { it.id }, includeAttachmentContext = true)
            }!!
        try {
            metrics.recordExternalCallTransactionState("conversation_v2_attachment")
            val generated = aiClient.generateConversationTurn(checkNotNull(preparation.aiRequest))
            saveGeneratedTurn(preparation, generated)
        } catch (exception: Exception) {
            failAttachedTurn(event, "AI_RESPONSE_GENERATION_FAILED")
            metrics.incrementConversationFailure()
            logger.error("첨부파일 회고 AI 응답 생성 실패 - retrospectiveId: ${event.retrospectiveId}, turnId: ${event.turnId}", exception)
        }
    }

    fun failAttachedTurn(
        event: AttachmentConversationRequestedEvent,
        errorCode: String,
    ) {
        runCatching {
            transactionTemplate.executeWithoutResult {
                turnRepository
                    .findAllByRetrospectiveIdOrderByTurnNumberAsc(event.retrospectiveId)
                    .find { it.id == event.turnId && it.status == ConversationTurnStatus.PROCESSING }
                    ?.also {
                        it.fail(errorCode)
                        turnRepository.save(it)
                    }
            }
        }.onFailure { logger.error("첨부파일 회고 실패 상태 저장 실패 - turnId: ${event.turnId}", it) }
    }

    fun completeUnreadableAttachedTurn(event: AttachmentConversationRequestedEvent) {
        transactionTemplate.executeWithoutResult {
            val retrospective = findV2(event.retrospectiveId, event.userId)
            val turn =
                turnRepository.findByUserMessageId(event.userMessageId)?.takeIf {
                    it.id == event.turnId &&
                        it.retrospectiveId == event.retrospectiveId &&
                        it.status == ConversationTurnStatus.PROCESSING
                }
                    ?: return@executeWithoutResult
            val userMessage = chatMessageRepository.findById(event.userMessageId) ?: error("사용자 메시지를 찾을 수 없습니다.")
            val hasUnreadableAttachment =
                attachmentRepository
                    .findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(event.userMessageId)
                    .any { it.analysisStatus == com.didit.domain.retrospect.AttachmentAnalysisStatus.UNREADABLE }
            check(hasUnreadableAttachment) { "읽을 수 없는 첨부파일이 없습니다." }

            userMessage.classify(MessageRelevance.BRIDGEABLE)
            chatMessageRepository.save(userMessage)
            val assistantMessage =
                chatMessageRepository.save(
                    ChatMessage.v2AssistantMessage(
                        retrospective = retrospective,
                        content = UNREADABLE_ATTACHMENT_GUIDE,
                        systemGuide = true,
                    ),
                )
            turn.complete(assistantMessage.id, null, 0, 0)
            turnRepository.save(turn)
        }
    }

    private fun prepareTurn(
        retrospectiveId: UUID,
        userId: UUID,
        clientMessageId: UUID,
        content: String,
        inputType: InputType,
        attachmentIds: List<UUID>,
    ): TurnPreparation =
        transactionTemplate.execute {
            val distinctAttachmentIds = attachmentIds.distinct()
            if (distinctAttachmentIds.size > 3 || distinctAttachmentIds.size != attachmentIds.size) {
                throw AttachmentLimitExceededException()
            }
            if (content.isBlank() && distinctAttachmentIds.isEmpty()) throw IllegalArgumentException("회고 내용 또는 첨부파일이 필요합니다.")
            val retrospective = findV2ForUpdate(retrospectiveId, userId)
            if (!retrospective.isConversationActive()) throw ConversationAlreadyFinishedException(retrospectiveId)

            val existing = turnRepository.findByRetrospectiveIdAndClientMessageId(retrospectiveId, clientMessageId)
            if (existing != null) {
                val userMessage = chatMessageRepository.findById(existing.userMessageId) ?: error("사용자 메시지를 찾을 수 없습니다.")
                val existingAttachmentIds =
                    attachmentRepository.findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(userMessage.id).map { it.id }.sorted()
                if (userMessage.content != content.trim() ||
                    userMessage.inputType != inputType ||
                    existingAttachmentIds != distinctAttachmentIds.sorted()
                ) {
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
                        val hasUnreadableAttachment =
                            attachmentRepository
                                .findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(userMessage.id)
                                .any { it.analysisStatus == com.didit.domain.retrospect.AttachmentAnalysisStatus.UNREADABLE }
                        if (hasUnreadableAttachment) {
                            throw com.didit.application.retrospect.exception.AttachmentInvalidFileException(
                                existingAttachmentIds.first(),
                            )
                        }
                        existing.retry()
                        turnRepository.save(existing)
                        metrics.incrementConversationRetry()
                        return@execute createPreparation(retrospective, existing, userMessage, existingAttachmentIds)
                    }
                }
            }

            if (turnRepository.findFirstByRetrospectiveIdAndStatus(retrospectiveId, ConversationTurnStatus.PROCESSING) != null) {
                metrics.incrementConversationConflict()
                throw AnotherConversationTurnInProgressException(retrospectiveId)
            }

            if (retrospective.isPending()) retrospective.startProgress()
            val attachments =
                if (distinctAttachmentIds.isEmpty()) {
                    emptyList()
                } else {
                    attachmentRepository.findAllByIdInAndUserIdAndRetrospectiveId(distinctAttachmentIds, userId, retrospectiveId).also {
                        if (it.size != distinctAttachmentIds.size) throw AttachmentNotFoundException(distinctAttachmentIds.first())
                        it.forEach { attachment ->
                            if (attachment.uploadStatus != com.didit.domain.retrospect.AttachmentUploadStatus.UPLOADED) {
                                throw AttachmentNotUploadedException(attachment.id)
                            }
                            if (attachment.chatMessageId !=
                                null
                            ) {
                                throw com.didit.application.retrospect.exception
                                    .AttachmentAlreadyBoundException(attachment.id)
                            }
                        }
                    }
                }
            val userMessage =
                if (attachments.isEmpty()) {
                    ChatMessage.v2UserMessage(retrospective, content, inputType)
                } else {
                    ChatMessage.v2UserMessageWithAttachments(retrospective, content, inputType)
                }
            retrospective.addMessage(userMessage)
            retrospectiveRepository.save(retrospective)
            attachments.forEach {
                it.bindTo(userMessage.id)
                attachmentRepository.save(it)
            }
            val turn =
                turnRepository.save(
                    RetrospectiveConversationTurn(
                        retrospectiveId = retrospectiveId,
                        clientMessageId = clientMessageId,
                        userMessageId = userMessage.id,
                        turnNumber = turnRepository.countByRetrospectiveId(retrospectiveId) + 1,
                    ),
                )
            createPreparation(retrospective, turn, userMessage, distinctAttachmentIds)
        }!!

    private fun createPreparation(
        retrospective: Retrospective,
        turn: RetrospectiveConversationTurn,
        userMessage: ChatMessage,
        attachmentIds: List<UUID> = emptyList(),
        includeAttachmentContext: Boolean = false,
    ): TurnPreparation {
        val user = userFinder.findByIdOrThrow(retrospective.userId)
        val messages = chatMessageRepository.findAllByRetrospectiveIdOrderByCreatedAtAsc(retrospective.id)
        val items = findOrInitializeItems(retrospective.id)
        val currentAttachments =
            attachmentRepository
                .findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(userMessage.id)
                .filter { it.analysisStatus == com.didit.domain.retrospect.AttachmentAnalysisStatus.COMPLETED }
        val currentAttachmentContexts = currentAttachments.map(AttachmentConversationContextMapper::from)
        val context =
            turnPolicy.trimContext(
                messages.map { message ->
                    ConversationContextMessage(
                        id = message.id,
                        sender = message.sender,
                        content = message.content,
                        attachments =
                            if (message.id == userMessage.id) {
                                currentAttachmentContexts
                            } else {
                                attachmentRepository
                                    .findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(message.id)
                                    .filter {
                                        it.analysisStatus ==
                                            com.didit.domain.retrospect.AttachmentAnalysisStatus.COMPLETED
                                    }.map(AttachmentConversationContextMapper::from)
                            },
                    )
                },
                maxContextCharacters,
            )
        return TurnPreparation(
            retrospectiveId = retrospective.id,
            userId = retrospective.userId,
            turnId = turn.id,
            userMessageId = userMessage.id,
            aiRequest =
                if (attachmentIds.isEmpty() || includeAttachmentContext) {
                    ConversationTurnAIRequest(
                        job = user.job,
                        experience = user.experience,
                        messages = context,
                        analysisItems = items.map { ConversationAnalysisItem(it.itemType, it.status, it.summary) },
                        currentMessageId = userMessage.id,
                        consecutiveIrrelevantCount =
                            turnPolicy.consecutiveIrrelevantCount(
                                messages.map { ConversationV2ConversationMessageSnapshot(it.id, it.sender, it.relevance) },
                                userMessage.id,
                            ),
                    )
                } else {
                    null
                },
            attachmentIds = attachmentIds,
            sensitiveDataDetected = attachmentIds.isNotEmpty() && currentAttachmentContexts.any { it.containsSensitiveData },
            unreadableAttachmentDetected =
                attachmentIds.isNotEmpty() &&
                    attachmentRepository
                        .findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(userMessage.id)
                        .any { it.analysisStatus == com.didit.domain.retrospect.AttachmentAnalysisStatus.UNREADABLE },
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

            val generatedContent =
                generated
                    .content()
                    .let { content ->
                        if (preparation.sensitiveDataDetected) sensitiveDataDetector.redact(content) else content
                    }
            val responseContent =
                listOfNotNull(
                    UNREADABLE_ATTACHMENT_GUIDE.takeIf { preparation.unreadableAttachmentDetected },
                    SENSITIVE_DATA_WARNING.takeIf { preparation.sensitiveDataDetected },
                    generatedContent,
                ).joinToString("\n")
            check(responseContent.isNotBlank()) { "AI 응답이 비어 있습니다." }
            val assistantSnapshot = turnPolicy.assistantSnapshot(generated).copy(content = responseContent)
            val assistantMessage =
                chatMessageRepository.save(
                    ChatMessage.v2AssistantMessage(
                        retrospective = retrospective,
                        content = assistantSnapshot.content,
                        messageType = assistantSnapshot.messageType,
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
        val messages =
            chatMessageRepository
                .findAllByRetrospectiveIdOrderByCreatedAtAsc(retrospectiveId)
                .map { ConversationV2ConversationMessageSnapshot(it.id, it.sender, it.relevance) }
        turnPolicy
            .applyAnalysisUpdates(
                items.values.map { ConversationV2AnalysisItemSnapshot(it.itemType, it.status, it.summary) },
                generated.analysisUpdates,
                messages,
            ).filter { it.applied }
            .forEach { decision ->
                val item = checkNotNull(items[decision.update.itemType])
                val after = checkNotNull(decision.after)
                item.update(after.status, after.summary.orEmpty())
                analysisItemRepository.save(item)
                val newEvidence =
                    decision.acceptedEvidenceMessageIds
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

    private fun ChatMessage.toResult(
        attachments: List<com.didit.domain.retrospect.RetrospectiveAttachment> = emptyList(),
    ): ConversationMessageResult =
        if (messageType == ConversationMessageType.INTRO) {
            ConversationMessageResult(
                id = id,
                sender = sender,
                messageType = messageType,
                title = content,
                body = supportingContent,
                createdAt = createdAt,
                attachments = attachments.map { it.toMessageAttachmentResult() },
            )
        } else {
            ConversationMessageResult(
                id = id,
                sender = sender,
                messageType = messageType,
                content = content,
                createdAt = createdAt,
                attachments = attachments.map { it.toMessageAttachmentResult() },
            )
        }

    private fun com.didit.domain.retrospect.RetrospectiveAttachment.toMessageAttachmentResult() =
        ConversationMessageAttachmentResult(
            id = id,
            filename = originalFilename,
            fileType = fileType,
            contentType = contentType,
            size = actualSize ?: expectedSize,
            uploadStatus = uploadStatus,
            analysisStatus = analysisStatus,
            analysisErrorCode = analysisErrorCode,
        )

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
    val attachmentIds: List<UUID> = emptyList(),
    val sensitiveDataDetected: Boolean = false,
    val unreadableAttachmentDetected: Boolean = false,
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
