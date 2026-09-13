package com.didit.application.retrospect

import com.didit.application.retrospect.required.ConversationAnalysisUpdate
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ConversationV2TurnPolicy {
    fun initialIntro(): ConversationV2MessageSnapshot =
        ConversationV2MessageSnapshot(
            content = "오늘 어떤 일을 하셨나요?",
            messageType = ConversationMessageType.INTRO,
            supportingContent = "오늘 진행한 일 중 하나를 떠올려, 작업 내용과 함께 결과나 상태도 같이 적어보세요.",
        )

    fun trimContext(
        messages: List<ConversationContextMessage>,
        maxCharacters: Int,
    ): List<ConversationContextMessage> {
        var used = 0
        return messages
            .asReversed()
            .takeWhile {
                used += it.content.length
                used <= maxCharacters
            }.asReversed()
    }

    fun consecutiveIrrelevantCount(
        messages: List<ConversationV2ConversationMessageSnapshot>,
        currentMessageId: UUID,
    ): Int =
        messages
            .filter { it.sender == Sender.USER && it.id != currentMessageId }
            .asReversed()
            .takeWhile { it.relevance != MessageRelevance.RETROSPECTIVE }
            .count { it.relevance != null }

    fun evidenceDecision(
        messages: List<ConversationV2ConversationMessageSnapshot>,
        requestedMessageIds: List<UUID>,
    ): ConversationV2EvidenceDecision {
        val validIds =
            messages
                .filter { it.sender == Sender.USER && it.relevance == MessageRelevance.RETROSPECTIVE }
                .map { it.id }
                .toSet()
        val acceptedMessageIds = requestedMessageIds.filter { it in validIds }
        return ConversationV2EvidenceDecision(
            acceptedMessageIds = acceptedMessageIds,
            rejectedMessageIds = requestedMessageIds.filterNot { it in validIds },
        )
    }

    fun assistantSnapshot(generated: GeneratedConversationTurn): ConversationV2MessageSnapshot =
        ConversationV2MessageSnapshot(
            content = generated.content(),
            messageType =
                if (generated.relevance == MessageRelevance.OFF_TOPIC || generated.relevance == MessageRelevance.SERVICE_HELP) {
                    ConversationMessageType.SYSTEM_GUIDE
                } else {
                    ConversationMessageType.CONVERSATION
                },
        )

    fun applyAnalysisUpdates(
        items: List<ConversationV2AnalysisItemSnapshot>,
        updates: List<ConversationAnalysisUpdate>,
        messages: List<ConversationV2ConversationMessageSnapshot>,
    ): List<ConversationV2AnalysisUpdateDecision> {
        val currentItems = items.associateBy { it.itemType }.toMutableMap()
        return updates.map { update ->
            val evidence = evidenceDecision(messages, update.evidenceMessageIds)
            val before = currentItems[update.itemType]
            val after =
                if (before != null && evidence.acceptedMessageIds.isNotEmpty()) {
                    before
                        .copy(
                            status = maxOf(before.status, update.status),
                            summary = update.summary.trim().takeIf { it.isNotEmpty() },
                        ).also { currentItems[update.itemType] = it }
                } else {
                    before
                }
            ConversationV2AnalysisUpdateDecision(
                update = update,
                acceptedEvidenceMessageIds = evidence.acceptedMessageIds,
                rejectedEvidenceMessageIds = evidence.rejectedMessageIds,
                before = before,
                after = after,
                applied = before != null && evidence.acceptedMessageIds.isNotEmpty(),
            )
        }
    }
}

data class ConversationV2MessageSnapshot(
    val content: String,
    val messageType: ConversationMessageType,
    val supportingContent: String? = null,
)

data class ConversationV2ConversationMessageSnapshot(
    val id: UUID,
    val sender: Sender,
    val relevance: MessageRelevance?,
)

data class ConversationV2AnalysisItemSnapshot(
    val itemType: RetrospectiveItemType,
    val status: RetrospectiveItemStatus,
    val summary: String?,
)

data class ConversationV2EvidenceDecision(
    val acceptedMessageIds: List<UUID>,
    val rejectedMessageIds: List<UUID>,
)

data class ConversationV2AnalysisUpdateDecision(
    val update: ConversationAnalysisUpdate,
    val acceptedEvidenceMessageIds: List<UUID>,
    val rejectedEvidenceMessageIds: List<UUID>,
    val before: ConversationV2AnalysisItemSnapshot?,
    val after: ConversationV2AnalysisItemSnapshot?,
    val applied: Boolean,
)
