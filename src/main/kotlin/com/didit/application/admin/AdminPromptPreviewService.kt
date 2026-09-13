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
import com.didit.application.retrospect.ConversationV2AnalysisItemSnapshot
import com.didit.application.retrospect.ConversationV2ConversationMessageSnapshot
import com.didit.application.retrospect.ConversationV2TurnPolicy
import com.didit.application.retrospect.required.ConversationAnalysisItem
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdminPromptPreviewService(
    private val promptRepository: PromptRepository,
    private val aiClient: AdminPromptPreviewAIClient,
    private val turnPolicy: ConversationV2TurnPolicy,
    private val maxContextCharacters: Int = 30_000,
) : AdminPromptPreview {
    override fun preview(command: AdminPromptPreviewCommand): AdminPromptPreviewResult {
        validateMessage(command.message)
        val template = resolveTemplate(command)
        val priorState = command.priorState ?: initialState()
        validateState(priorState, command.userMessageId)

        val userMessage =
            AdminPromptPreviewMessage(
                id = command.userMessageId,
                sender = Sender.USER,
                content = command.message,
                messageType = ConversationMessageType.CONVERSATION,
            )
        val messagesBeforeGeneration = priorState.messages + userMessage
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
                        messagesBeforeGeneration.map { ConversationV2ConversationMessageSnapshot(it.id, it.sender, it.relevance) },
                        command.userMessageId,
                    ),
            )
        val generated = aiClient.preview(template, aiRequest)
        val classifiedMessages =
            messagesBeforeGeneration.map { message ->
                if (message.id == command.userMessageId) message.copy(relevance = generated.relevance) else message
            }
        val analysisItems =
            applyAnalysisUpdates(priorState.analysisItems, generated.relevance, generated.analysisUpdates, classifiedMessages)
        val assistantSnapshot = turnPolicy.assistantSnapshot(generated)
        val assistantMessage =
            AdminPromptPreviewMessage(
                id = UUID.randomUUID(),
                sender = Sender.AI,
                content = assistantSnapshot.content,
                messageType = assistantSnapshot.messageType,
                supportingContent = assistantSnapshot.supportingContent,
            )
        val nextState = AdminPromptPreviewState(classifiedMessages + assistantMessage, analysisItems)

        return AdminPromptPreviewResult(
            assistantMessage = assistantMessage,
            progress =
                AdminPromptPreviewProgress(
                    filledCount = analysisItems.count { it.status != RetrospectiveItemStatus.EMPTY },
                    totalCount = RetrospectiveItemType.entries.size,
                ),
            nextState = nextState,
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
    }

    private fun applyAnalysisUpdates(
        items: List<AdminPromptPreviewAnalysisItem>,
        relevance: MessageRelevance,
        updates: List<com.didit.application.retrospect.required.ConversationAnalysisUpdate>,
        messages: List<AdminPromptPreviewMessage>,
    ): List<AdminPromptPreviewAnalysisItem> {
        if (relevance != MessageRelevance.RETROSPECTIVE) return items
        val currentItems = items.associateBy { it.itemType }.toMutableMap()
        turnPolicy
            .applyAnalysisUpdates(
                items.map { ConversationV2AnalysisItemSnapshot(it.itemType, it.status, it.summary) },
                updates,
                messages.map { ConversationV2ConversationMessageSnapshot(it.id, it.sender, it.relevance) },
            ).filter { it.applied }
            .forEach { decision ->
                val after = checkNotNull(decision.after)
                currentItems[after.itemType] = AdminPromptPreviewAnalysisItem(after.itemType, after.status, after.summary)
            }
        return items.map { checkNotNull(currentItems[it.itemType]) }
    }
}
