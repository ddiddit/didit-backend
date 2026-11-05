package com.didit.application.admin

import com.didit.application.admin.provided.AdminPromptPreview
import com.didit.application.admin.provided.AdminPromptPreviewAnalysisItem
import com.didit.application.admin.provided.AdminPromptPreviewCommand
import com.didit.application.admin.provided.AdminPromptPreviewMessage
import com.didit.application.admin.provided.AdminPromptPreviewProgress
import com.didit.application.admin.provided.AdminPromptPreviewResult
import com.didit.application.admin.provided.AdminPromptPreviewState
import com.didit.application.admin.provided.AdminPromptSource
import com.didit.application.admin.required.AdminPromptPreviewAIClient
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import com.didit.application.prompt.required.PromptRepository
import com.didit.application.retrospect.ConversationAnswerGuide
import com.didit.application.retrospect.ConversationAnswerGuidePolicy
import com.didit.application.retrospect.ConversationV2AnalysisItemSnapshot
import com.didit.application.retrospect.ConversationV2ConversationMessageSnapshot
import com.didit.application.retrospect.ConversationV2TurnPolicy
import com.didit.application.retrospect.dto.ConversationAnswerGuideResult
import com.didit.application.retrospect.required.ConversationAnalysisItem
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationTurnAction
import com.didit.domain.retrospect.ConversationUserIntent
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveConversationMode
import com.didit.domain.retrospect.RetrospectiveItemProgressStatus
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdminPromptPreviewService(
    private val promptRepository: PromptRepository,
    private val aiClient: AdminPromptPreviewAIClient,
    private val turnPolicy: ConversationV2TurnPolicy,
    private val answerGuidePolicy: ConversationAnswerGuidePolicy,
    @param:Value("\${retrospective.v2.max-context-characters:30000}")
    private val maxContextCharacters: Int = 30_000,
) : AdminPromptPreview {
    companion object {
        private const val EXTENSION_STARTED_GUIDE = "좋아요. 이어서 말씀해주세요."
        private const val FINAL_EXTENSION_COMPLETION_GUIDE =
            "충분히 이야기 나눴어요. 지금까지 추가한 내용까지 포함해 회고를 정리할게요."
        private const val EXTENSION_EMPTY_GUIDE = "알겠어요. 이어서 말씀해주시면 함께 정리해볼게요."
        private const val EXTENSION_COMPLETION_GUIDE = "새로 이야기한 내용까지 포함해 회고를 마칠 수 있어요."
    }

    override fun preview(command: AdminPromptPreviewCommand): AdminPromptPreviewResult {
        validateMessage(command.message)
        val template = resolveTemplate(command)
        val priorState = command.priorState ?: initialState()
        validateState(priorState, command.userMessageId)
        if (
            command.conversationIntent == ConversationUserIntent.CONTINUE_AFTER_COMPLETION &&
            priorState.completionPreviouslyOffered
        ) {
            return continueExtensionPreview(priorState, command)
        }

        val userMessage =
            AdminPromptPreviewMessage(
                id = command.userMessageId,
                sender = Sender.USER,
                content = command.message,
                messageType = ConversationMessageType.CONVERSATION,
            )
        val messagesBeforeGeneration = priorState.messages + userMessage
        val priorItemSnapshots = priorState.analysisItems.map { it.toSnapshot() }
        val initialEligibleQuestionTargets =
            turnPolicy.eligibleQuestionTargets(
                items = priorItemSnapshots,
                recentQuestionTargets = priorState.recentQuestionTargets,
                questionTargetCounts = priorState.askedQuestionTargets.associateWith { 1 },
            )
        val continuationRequested =
            command.conversationIntent == ConversationUserIntent.CONTINUE_AFTER_COMPLETION &&
                priorState.completionPreviouslyOffered
        val aiRequest =
            ConversationTurnAIRequest(
                job = command.job,
                experience = command.experience,
                messages =
                    turnPolicy.trimContext(
                        messagesBeforeGeneration.map { ConversationContextMessage(it.id, it.sender, it.content) },
                        maxContextCharacters,
                    ),
                analysisItems = priorState.analysisItems.map { ConversationAnalysisItem(it.itemType, it.status, it.summary) },
                currentMessageId = command.userMessageId,
                consecutiveIrrelevantCount =
                    turnPolicy.consecutiveIrrelevantCount(
                        messagesBeforeGeneration.map {
                            ConversationV2ConversationMessageSnapshot(it.id, it.sender, it.relevance, it.content)
                        },
                        command.userMessageId,
                    ),
                eligibleQuestionTargets = initialEligibleQuestionTargets,
                recentQuestionTargets = priorState.recentQuestionTargets,
                completionRecommended =
                    !continuationRequested &&
                        turnPolicy.shouldRecommendCompletion(priorItemSnapshots),
                completionPreviouslyOffered = priorState.completionPreviouslyOffered,
                continuationRequested = continuationRequested,
            )
        val generated = aiClient.preview(template, aiRequest)
        val currentQuestionTarget =
            priorState.currentQuestionTarget
                ?: priorState.recentQuestionTargets.lastOrNull().takeIf { priorState.consecutiveAskCount > 0 }
        val policyGenerated =
            turnPolicy.normalizeItemDecline(
                generated = generated,
                lastQuestionTarget = currentQuestionTarget,
                userMessageContent = command.message,
            )
        val classifiedMessages =
            messagesBeforeGeneration.map { message ->
                if (message.id == command.userMessageId) message.copy(relevance = generated.relevance) else message
            }
        val analysisUpdateResult =
            applyAnalysisUpdates(
                priorState.analysisItems,
                policyGenerated.relevance,
                policyGenerated.analysisUpdates,
                classifiedMessages,
                command.userMessageId,
            )
        val analyzedItems = analysisUpdateResult.items
        val updatedItemTypes = analysisUpdateResult.currentMessageUpdatedItemTypes
        val analysisItems =
            analyzedItems.applyDeclinedQuestion(currentQuestionTarget, policyGenerated.declinedItemTypes)
        val updatedItemSnapshots = analysisItems.map { it.toSnapshot() }
        val readyToComplete = turnPolicy.readyToComplete(updatedItemSnapshots)
        val standardQuestionTargets =
            turnPolicy.eligibleQuestionTargets(
                items = updatedItemSnapshots,
                recentQuestionTargets = priorState.recentQuestionTargets,
                questionTargetCounts = priorState.askedQuestionTargets.associateWith { 1 },
            )
        val eligibleQuestionTargets =
            if (standardQuestionTargets.isEmpty() && updatedItemTypes.isNotEmpty() && !readyToComplete) {
                turnPolicy.minimumCompletionQuestionTargets(updatedItemSnapshots)
            } else {
                standardQuestionTargets
            }
        var extensionMeaningfulTurnCount = priorState.extensionMeaningfulTurnCount
        var extensionConsecutiveEmptyCount = priorState.extensionConsecutiveEmptyCount
        val decided =
            if (priorState.conversationMode == RetrospectiveConversationMode.EXTENDING) {
                val extensionDecision =
                    decideExtensionPreview(
                        generated = policyGenerated,
                        userMessageContent = command.message,
                        readyToComplete = readyToComplete,
                        updatedItemTypes = updatedItemTypes,
                        extensionRoundCount = priorState.extensionRoundCount,
                        meaningfulTurnCount = extensionMeaningfulTurnCount,
                        consecutiveEmptyCount = extensionConsecutiveEmptyCount,
                    )
                extensionMeaningfulTurnCount = extensionDecision.meaningfulTurnCount
                extensionConsecutiveEmptyCount = extensionDecision.consecutiveEmptyCount
                extensionDecision.generated
            } else {
                turnPolicy.decideTurn(
                    generated = policyGenerated,
                    eligibleQuestionTargets = eligibleQuestionTargets,
                    completionRecommended =
                        !continuationRequested &&
                            turnPolicy.shouldRecommendCompletion(updatedItemSnapshots),
                    completionPreviouslyOffered = priorState.completionPreviouslyOffered,
                    continuationRequested = continuationRequested,
                    readyToComplete = readyToComplete,
                    userMessageContent = command.message,
                    previousQuestionTarget = currentQuestionTarget,
                    insufficientGuidancePreviouslyShown =
                        priorState.messages.any { message ->
                            message.sender == Sender.AI && turnPolicy.isInsufficientCompletionGuide(message.content)
                        },
                )
            }
        val answerGuide =
            answerGuidePolicy
                .decide(
                    generated = policyGenerated,
                    decided = decided,
                    items = updatedItemSnapshots,
                    sensitiveDataDetected = false,
                    previouslyAskedTargets = priorState.askedQuestionTargets,
                    userMessageContent = command.message,
                )?.toResult()
        val assistantMessage =
            decided.content().takeIf(String::isNotBlank)?.let {
                val assistantSnapshot = turnPolicy.assistantSnapshot(decided)
                AdminPromptPreviewMessage(
                    id = UUID.randomUUID(),
                    sender = Sender.AI,
                    content = assistantSnapshot.content,
                    messageType = assistantSnapshot.messageType,
                    supportingContent = assistantSnapshot.supportingContent,
                    answerGuide = answerGuide,
                )
            }
        val nextState =
            AdminPromptPreviewState(
                messages = classifiedMessages + listOfNotNull(assistantMessage),
                analysisItems = analysisItems,
                currentQuestionTarget = decided.questionTarget,
                recentQuestionTargets =
                    if (decided.action == ConversationTurnAction.ASK) {
                        (priorState.recentQuestionTargets + checkNotNull(decided.questionTarget)).takeLast(3)
                    } else {
                        priorState.recentQuestionTargets
                    },
                askedQuestionTargets =
                    decided.questionTarget?.let { priorState.askedQuestionTargets + it } ?: priorState.askedQuestionTargets,
                completedQuestionCount =
                    priorState.completedQuestionCount + if (decided.action == ConversationTurnAction.ASK) 1 else 0,
                consecutiveAskCount =
                    if (decided.action == ConversationTurnAction.ASK) priorState.consecutiveAskCount + 1 else 0,
                completionPreviouslyOffered =
                    priorState.completionPreviouslyOffered || decided.action == ConversationTurnAction.OFFER_COMPLETION,
                conversationMode = priorState.conversationMode,
                extensionRoundCount = priorState.extensionRoundCount,
                extensionMeaningfulTurnCount = extensionMeaningfulTurnCount,
                extensionConsecutiveEmptyCount = extensionConsecutiveEmptyCount,
            )

        return AdminPromptPreviewResult(
            assistantMessage = assistantMessage,
            progress =
                AdminPromptPreviewProgress(
                    filledCount = analysisItems.count { it.status != RetrospectiveItemStatus.EMPTY },
                    totalCount = RetrospectiveItemType.entries.size,
                ),
            nextState = nextState,
            canContinue = nextState.extensionRoundCount < 3,
        )
    }

    private fun decideExtensionPreview(
        generated: GeneratedConversationTurn,
        userMessageContent: String,
        readyToComplete: Boolean,
        updatedItemTypes: Set<RetrospectiveItemType>,
        extensionRoundCount: Int,
        meaningfulTurnCount: Int,
        consecutiveEmptyCount: Int,
    ): PreviewExtensionDecision {
        turnPolicy.decideExplicitCompletion(generated, userMessageContent, readyToComplete)?.let {
            return PreviewExtensionDecision(it, meaningfulTurnCount, consecutiveEmptyCount)
        }
        if (generated.relevance == MessageRelevance.OFF_TOPIC || generated.relevance == MessageRelevance.SERVICE_HELP) {
            return PreviewExtensionDecision(
                generated.copy(action = ConversationTurnAction.REFLECT, question = null, questionTarget = null),
                meaningfulTurnCount,
                consecutiveEmptyCount,
            )
        }
        val nextMeaningful = meaningfulTurnCount + if (updatedItemTypes.isNotEmpty()) 1 else 0
        val nextEmpty = if (updatedItemTypes.isNotEmpty()) 0 else consecutiveEmptyCount + 1
        val completionReached = nextMeaningful >= 2 || nextEmpty >= 2
        val decided =
            when {
                completionReached ->
                    generated.copy(
                        action = ConversationTurnAction.OFFER_COMPLETION,
                        acknowledgement =
                            if (extensionRoundCount < 3) EXTENSION_COMPLETION_GUIDE else FINAL_EXTENSION_COMPLETION_GUIDE,
                        interpretation = "",
                        question = null,
                        questionTarget = null,
                    )
                updatedItemTypes.isEmpty() ->
                    generated.copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = EXTENSION_EMPTY_GUIDE,
                        interpretation = "",
                        question = null,
                        questionTarget = null,
                    )
                else -> {
                    val reflected = generated.copy(action = ConversationTurnAction.REFLECT, question = null, questionTarget = null)
                    if (reflected.content().isBlank()) {
                        reflected.copy(acknowledgement = "말씀해주신 내용을 회고에 반영했어요.")
                    } else {
                        reflected
                    }
                }
            }
        return PreviewExtensionDecision(decided, nextMeaningful, nextEmpty)
    }

    private data class PreviewExtensionDecision(
        val generated: GeneratedConversationTurn,
        val meaningfulTurnCount: Int,
        val consecutiveEmptyCount: Int,
    )

    private fun continueExtensionPreview(
        priorState: AdminPromptPreviewState,
        command: AdminPromptPreviewCommand,
    ): AdminPromptPreviewResult {
        val canEnter = priorState.extensionRoundCount < 3
        val userMessage =
            AdminPromptPreviewMessage(
                id = command.userMessageId,
                sender = Sender.USER,
                content = command.message,
                messageType = ConversationMessageType.CONVERSATION,
                relevance = MessageRelevance.BRIDGEABLE,
            )
        val assistantMessage =
            AdminPromptPreviewMessage(
                id = UUID.randomUUID(),
                sender = Sender.AI,
                content = if (canEnter) EXTENSION_STARTED_GUIDE else FINAL_EXTENSION_COMPLETION_GUIDE,
                messageType = ConversationMessageType.CONVERSATION,
            )
        val nextState =
            priorState.copy(
                messages = priorState.messages + userMessage + assistantMessage,
                currentQuestionTarget = null,
                conversationMode = RetrospectiveConversationMode.EXTENDING,
                extensionRoundCount = priorState.extensionRoundCount + if (canEnter) 1 else 0,
                extensionMeaningfulTurnCount = if (canEnter) 0 else priorState.extensionMeaningfulTurnCount,
                extensionConsecutiveEmptyCount = if (canEnter) 0 else priorState.extensionConsecutiveEmptyCount,
                completionPreviouslyOffered = true,
            )
        return AdminPromptPreviewResult(
            assistantMessage = assistantMessage,
            progress =
                AdminPromptPreviewProgress(
                    filledCount = priorState.analysisItems.count { it.status != RetrospectiveItemStatus.EMPTY },
                    totalCount = RetrospectiveItemType.entries.size,
                ),
            nextState = nextState,
            canContinue = nextState.extensionRoundCount < 3,
        )
    }

    private fun resolveTemplate(command: AdminPromptPreviewCommand): String =
        when (command.promptSource) {
            AdminPromptSource.SAVED ->
                promptRepository
                    .findByJobTypeAndPromptType(PromptJobType.valueOf(command.job.name), PromptType.CONVERSATION_V2)
                    ?.content
                    ?: throw BusinessException(ErrorCode.NOT_FOUND, "V2 대화 프롬프트를 찾을 수 없습니다.")
            AdminPromptSource.DRAFT ->
                command.draftPrompt?.takeIf { it.isNotBlank() }
                    ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "프롬프트 초안은 비어 있을 수 없습니다.")
        }

    private fun initialState(): AdminPromptPreviewState {
        val intro = turnPolicy.initialIntro()
        return AdminPromptPreviewState(
            messages =
                listOf(
                    AdminPromptPreviewMessage(
                        id = UUID.randomUUID(),
                        sender = Sender.AI,
                        content = intro.content,
                        messageType = intro.messageType,
                        supportingContent = intro.supportingContent,
                    ),
                ),
            analysisItems = RetrospectiveItemType.entries.map { AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null) },
        )
    }

    private fun validateMessage(message: String) {
        if (message.isBlank()) throw BusinessException(ErrorCode.INVALID_REQUEST, "회고 내용은 비어 있을 수 없습니다.")
    }

    private fun validateState(
        state: AdminPromptPreviewState,
        userMessageId: UUID,
    ) {
        if (state.messages
                .map { it.id }
                .toSet()
                .size != state.messages.size ||
            state.messages.any { it.id == userMessageId }
        ) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "대화 메시지 ID가 중복되었습니다.")
        }
        if (state.analysisItems.map { it.itemType }.toSet() != RetrospectiveItemType.entries.toSet() ||
            state.analysisItems.size != RetrospectiveItemType.entries.size
        ) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "분석 항목은 일곱 개 항목을 각각 하나씩 포함해야 합니다.")
        }
        if (state.completedQuestionCount < 0 || state.consecutiveAskCount < 0 || state.recentQuestionTargets.size > 3) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "대화 정책 상태가 올바르지 않습니다.")
        }
    }

    private fun applyAnalysisUpdates(
        items: List<AdminPromptPreviewAnalysisItem>,
        relevance: MessageRelevance,
        updates: List<com.didit.application.retrospect.required.ConversationAnalysisUpdate>,
        messages: List<AdminPromptPreviewMessage>,
        currentUserMessageId: UUID,
    ): PreviewAnalysisUpdateResult {
        if (relevance != MessageRelevance.RETROSPECTIVE) return PreviewAnalysisUpdateResult(items, emptySet())
        val currentItems = items.associateBy { it.itemType }.toMutableMap()
        val decisions =
            turnPolicy.applyAnalysisUpdates(
                items.map { it.toSnapshot() },
                updates,
                messages.map { ConversationV2ConversationMessageSnapshot(it.id, it.sender, it.relevance, it.content) },
            )
        decisions
            .filter { it.applied }
            .forEach { decision ->
                val after = checkNotNull(decision.after)
                val currentMessageChanged =
                    decision.before != decision.after && currentUserMessageId in decision.acceptedEvidenceMessageIds
                currentItems[after.itemType] =
                    AdminPromptPreviewAnalysisItem(
                        after.itemType,
                        after.status,
                        after.summary,
                        questionAllowed = true,
                        progressStatus =
                            if (currentMessageChanged) {
                                RetrospectiveItemProgressStatus.ANSWERED
                            } else {
                                after.progressStatus
                            },
                    )
            }
        val updatedItemTypes =
            decisions
                .filter {
                    it.applied &&
                        it.before != it.after &&
                        currentUserMessageId in it.acceptedEvidenceMessageIds
                }.map { it.update.itemType }
                .toSet()
        return PreviewAnalysisUpdateResult(
            items = items.map { checkNotNull(currentItems[it.itemType]) },
            currentMessageUpdatedItemTypes = updatedItemTypes,
        )
    }

    private data class PreviewAnalysisUpdateResult(
        val items: List<AdminPromptPreviewAnalysisItem>,
        val currentMessageUpdatedItemTypes: Set<RetrospectiveItemType>,
    )

    private fun AdminPromptPreviewAnalysisItem.toSnapshot() =
        ConversationV2AnalysisItemSnapshot(itemType, status, summary, questionAllowed, progressStatus)

    private fun retrospectiveMessageCount(messages: List<AdminPromptPreviewMessage>): Int =
        messages.count { it.sender == Sender.USER && it.relevance == MessageRelevance.RETROSPECTIVE }

    private fun ConversationAnswerGuide.toResult() = ConversationAnswerGuideResult(mode = mode, examples = examples)

    private fun List<AdminPromptPreviewAnalysisItem>.applyDeclinedQuestion(
        lastQuestionTarget: RetrospectiveItemType?,
        declinedItemTypes: List<RetrospectiveItemType>,
    ): List<AdminPromptPreviewAnalysisItem> {
        if (lastQuestionTarget == null || lastQuestionTarget !in declinedItemTypes) return this
        return map { item ->
            if (item.itemType == lastQuestionTarget) {
                item.copy(questionAllowed = false, progressStatus = RetrospectiveItemProgressStatus.SKIPPED)
            } else {
                item
            }
        }
    }
}
